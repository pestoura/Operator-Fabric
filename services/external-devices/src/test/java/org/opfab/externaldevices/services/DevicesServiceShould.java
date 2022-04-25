/* Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */

package org.opfab.externaldevices.services;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opfab.externaldevices.configuration.externaldevices.ExternalDevicesWatchdogProperties;
import org.opfab.externaldevices.drivers.ExternalDeviceConfigurationException;
import org.opfab.externaldevices.drivers.ExternalDeviceDriver;
import org.opfab.externaldevices.drivers.ExternalDeviceDriverException;
import org.opfab.externaldevices.drivers.ExternalDeviceDriverFactory;
import org.opfab.externaldevices.model.Device;
import org.opfab.externaldevices.model.DeviceConfigurationData;
import org.opfab.externaldevices.model.ResolvedConfiguration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicesServiceShould {

    // We use IP addresses for hosts rather than names for tests because otherwise we would need to mock the static
    // method InetAddress.getByName.
    // This also means that we can't test the part of the code that handles a change in the resolved ip after a driver
    // is created.

    DevicesService devicesService;

    @Mock
    private ConfigService configService;

    @Mock
    private ExternalDeviceDriverFactory externalDeviceDriverFactory;

    @Mock
    private ExternalDevicesWatchdogProperties externalDevicesWatchdogProperties;

    @BeforeEach
    public void setUp() {
        devicesService = new DevicesService(configService,externalDeviceDriverFactory,externalDevicesWatchdogProperties);
    }

    @Test
    void connectDeviceIfCorrectConfig() throws ExternalDeviceConfigurationException, UnknownHostException, ExternalDeviceDriverException {

        sharedExternalDriverMockConfiguration();

        // Call the connectDevice method twice on the same device
        devicesService.connectDevice("testDeviceId");
        devicesService.connectDevice("testDeviceId");

        // The corresponding configuration should have been retrieved each time (to check for updates)
        verify(configService,times(2)).retrieveDeviceConfiguration("testDeviceId");

        // But the corresponding driver should only have been created once (because for the second call it's already in the driver pool)
        verify(externalDeviceDriverFactory,times(1)).create("123.45.67.1",1234);

    }

    @Test
    void connectDeviceWhenConfigIsUpdated() throws ExternalDeviceConfigurationException, UnknownHostException, ExternalDeviceDriverException {

        DeviceConfigurationData initialDeviceConfigurationData = buildDeviceConfiguration(1234, "testDeviceId");
        DeviceConfigurationData updatedDeviceConfigurationData = buildDeviceConfiguration(5678, "testDeviceId");

        ExternalDeviceDriver initialExternalDeviceDriver = mock(ExternalDeviceDriver.class);
        ExternalDeviceDriver updatedExternalDeviceDriver = mock(ExternalDeviceDriver.class);

        // Mock configuration
        when(initialExternalDeviceDriver.getResolvedHost()).thenReturn(InetAddress.getByName("123.45.67.1"));
        when(initialExternalDeviceDriver.getPort()).thenReturn(1234);
        when(configService.retrieveDeviceConfiguration("testDeviceId")).thenReturn(initialDeviceConfigurationData);
        when(externalDeviceDriverFactory.create("123.45.67.1",1234)).thenReturn(initialExternalDeviceDriver);
        when(externalDeviceDriverFactory.create("123.45.67.1",5678)).thenReturn(updatedExternalDeviceDriver);

        // Call the connectDevice method twice on the same device, but the configuration changes after the first call
        devicesService.connectDevice("testDeviceId");
        when(configService.retrieveDeviceConfiguration("testDeviceId")).thenReturn(updatedDeviceConfigurationData);
        devicesService.connectDevice("testDeviceId");

        // The corresponding configuration should have been retrieved each time (to check for updates)
        verify(configService,times(2)).retrieveDeviceConfiguration("testDeviceId");

        // Driver creation should happen twice
        verify(externalDeviceDriverFactory,times(1)).create("123.45.67.1",1234);
        verify(externalDeviceDriverFactory,times(1)).create("123.45.67.1",5678);

    }

    @Test
    void connectAndDisconnectDriverAssociatedWithDevice() throws ExternalDeviceConfigurationException, ExternalDeviceDriverException {

        DeviceConfigurationData deviceConfigurationData = buildDeviceConfiguration(1234, "testDeviceId");

        ExternalDeviceDriver externalDeviceDriver = mock(ExternalDeviceDriver.class);

        // Mock configuration
        when(configService.retrieveDeviceConfiguration("testDeviceId")).thenReturn(deviceConfigurationData);
        when(externalDeviceDriverFactory.create("123.45.67.1",1234)).thenReturn(externalDeviceDriver);

        devicesService.connectDevice("testDeviceId");
        verify(externalDeviceDriver,times(1)).connect();

        devicesService.disconnectDevice("testDeviceId");
        verify(externalDeviceDriver,times(1)).disconnect();

        // In the case of a disconnect, there is no need to check configuration again
        verify(configService,times(1)).retrieveDeviceConfiguration("testDeviceId");

    }

    @Test
    void sendAppropriateSignalIfCorrectConfiguration() throws ExternalDeviceConfigurationException, ExternalDeviceDriverException {

        DeviceConfigurationData deviceConfigurationData1 = buildDeviceConfiguration(1234, "testDeviceId");
        ResolvedConfiguration resolvedConfiguration1 = new ResolvedConfiguration(deviceConfigurationData1,3);
        ExternalDeviceDriver externalDeviceDriver1 = mock(ExternalDeviceDriver.class);

        DeviceConfigurationData deviceConfigurationData2 = buildDeviceConfiguration(5678, "testDeviceId2");
        ResolvedConfiguration resolvedConfiguration2 = new ResolvedConfiguration(deviceConfigurationData2,4);
        ExternalDeviceDriver externalDeviceDriver2 = mock(ExternalDeviceDriver.class);

        // Mock configuration
        when(configService.getResolvedConfigurationList("ALARM", "testUser")).thenReturn(Arrays.asList(resolvedConfiguration1, resolvedConfiguration2));
        when(externalDeviceDriverFactory.create("123.45.67.1",1234)).thenReturn(externalDeviceDriver1);
        when(externalDeviceDriverFactory.create("123.45.67.1",5678)).thenReturn(externalDeviceDriver2);

        devicesService.sendSignalToAllDevicesOfUser("ALARM", "testUser");

        verify(configService,times(1)).getResolvedConfigurationList("ALARM", "testUser");
        verify(externalDeviceDriver1,times(1)).send(3);
        verify(externalDeviceDriver2,times(1)).send(4);
    }

    @Test
    void sendWatchdogToConnectedDevicesSignalIfEnabled() throws ExternalDeviceConfigurationException, ExternalDeviceDriverException {

        final int CUSTOM_SIGNAL_ID = 4;

        DeviceConfigurationData deviceConfigurationData = buildDeviceConfiguration(1234, "testDeviceId");

        ExternalDeviceDriver externalDeviceDriver = mock(ExternalDeviceDriver.class);

        // Mock configuration
        when(configService.retrieveDeviceConfiguration("testDeviceId")).thenReturn(deviceConfigurationData);
        when(externalDeviceDriverFactory.create("123.45.67.1",1234)).thenReturn(externalDeviceDriver);
        when(externalDevicesWatchdogProperties.getEnabled()).thenReturn(true);
        when(externalDevicesWatchdogProperties.getSignalId()).thenReturn(CUSTOM_SIGNAL_ID);

        devicesService.connectDevice("testDeviceId"); //Necessary to add driver to pool

        //If connected
        when(externalDeviceDriver.isConnected()).thenReturn(true);
        devicesService.sendWatchdog();
        verify(externalDeviceDriver,times(1)).send(CUSTOM_SIGNAL_ID);
        clearInvocations(externalDeviceDriver);

        //If not connected
        when(externalDeviceDriver.isConnected()).thenReturn(false);
        devicesService.sendWatchdog();
        verify(externalDeviceDriver,times(0)).send(CUSTOM_SIGNAL_ID);

    }

    @Test
    void notSendWatchdogSignalToDevicesIfDisabled() throws ExternalDeviceConfigurationException, ExternalDeviceDriverException {

        DeviceConfigurationData deviceConfigurationData = buildDeviceConfiguration(1234, "testDeviceId");

        ExternalDeviceDriver externalDeviceDriver = mock(ExternalDeviceDriver.class);

        // Mock configuration
        when(configService.retrieveDeviceConfiguration("testDeviceId")).thenReturn(deviceConfigurationData);
        when(externalDeviceDriverFactory.create("123.45.67.1",1234)).thenReturn(externalDeviceDriver);
        when(externalDevicesWatchdogProperties.getEnabled()).thenReturn(false);

        devicesService.connectDevice("testDeviceId"); //Necessary to add driver to pool

        devicesService.sendWatchdog();
        verify(externalDeviceDriver,times(0)).send(anyInt());

    }

    @Test
    void getDeviceIfExists() throws ExternalDeviceDriverException, ExternalDeviceConfigurationException, UnknownHostException {

        ExternalDeviceDriver externalDeviceDriver = sharedExternalDriverMockConfiguration();
        when(externalDeviceDriver.isConnected()).thenReturn(true);

        // To add the device to the driver pool
        devicesService.connectDevice("testDeviceId");

        Optional<Device> result = devicesService.getDevice("testDeviceId");
        Assertions.assertThat(result).isPresent();
        Device device = result.get();
        Assertions.assertThat(device.getId()).isEqualTo("testDeviceId");
        Assertions.assertThat(device.getResolvedAddress()).isEqualTo("/123.45.67.1");
        Assertions.assertThat(device.getPort()).isEqualTo(1234);
        Assertions.assertThat(device.getIsConnected()).isTrue();

    }

    @Test
    void returnEmptyIfDeviceDoesntExists() {

        Optional<Device> result = devicesService.getDevice("deviceThatIsNotInPool");
        Assertions.assertThat(result).isEmpty();

    }

    @Test
    void getDevices() throws ExternalDeviceDriverException, ExternalDeviceConfigurationException, UnknownHostException {

        ExternalDeviceDriver externalDeviceDriver = sharedExternalDriverMockConfiguration();
        when(externalDeviceDriver.isConnected()).thenReturn(true);

        // To add the device to the driver pool
        devicesService.connectDevice("testDeviceId");

        List<Device> deviceList = devicesService.getDevices();
        Assertions.assertThat(deviceList).isNotNull().hasSize(1);
        Device device = deviceList.get(0);
        Assertions.assertThat(device.getId()).isEqualTo("testDeviceId");
        Assertions.assertThat(device.getResolvedAddress()).isEqualTo("/123.45.67.1");
        Assertions.assertThat(device.getPort()).isEqualTo(1234);
        Assertions.assertThat(device.getIsConnected()).isTrue();

    }

    @Test
    void returnEmptyDevicesList() {

        List<Device> deviceList = devicesService.getDevices();
        Assertions.assertThat(deviceList).isNotNull().isEmpty();

    }

    /** This method contains the basic configuration of mock ConfigService, ExternalDeviceDriverFactory and
     * ExternalDeviceDriver instances that is common to several tests.
     *
     * @return mock ExternalDeviceDriver for further test-specific configuration
     * */
    private ExternalDeviceDriver sharedExternalDriverMockConfiguration() throws ExternalDeviceConfigurationException, UnknownHostException, ExternalDeviceDriverException {

        DeviceConfigurationData deviceConfigurationData = buildDeviceConfiguration(1234, "testDeviceId");

        ExternalDeviceDriver externalDeviceDriver = mock(ExternalDeviceDriver.class);

        // Mock configuration
        when(externalDeviceDriver.getResolvedHost()).thenReturn(InetAddress.getByName("123.45.67.1"));
        when(externalDeviceDriver.getPort()).thenReturn(1234);
        when(configService.retrieveDeviceConfiguration("testDeviceId")).thenReturn(deviceConfigurationData);
        when(externalDeviceDriverFactory.create("123.45.67.1",1234)).thenReturn(externalDeviceDriver);

        return externalDeviceDriver;

    }

    private DeviceConfigurationData buildDeviceConfiguration(int port, String deviceId) {
        return DeviceConfigurationData.builder()
                .id(deviceId)
                .host("123.45.67.1")
                .port(port)
                .signalMappingId("testSignalMapping")
                .build();
    }
}
