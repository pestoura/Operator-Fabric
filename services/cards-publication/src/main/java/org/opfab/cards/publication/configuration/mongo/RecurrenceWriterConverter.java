/* Copyright (c) 2018-2022, RTE (http://www.rte-france.com)
 * See AUTHORS.txt
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 * This file is part of the OperatorFabric project.
 */



package org.opfab.cards.publication.configuration.mongo;

import org.bson.Document;
import org.opfab.cards.publication.model.RecurrencePublicationData;
import org.opfab.cards.publication.model.HoursAndMinutes;


import java.util.List;

public class RecurrenceWriterConverter { 

    private static String defaultTimeZone = "Europe/Paris";

    private RecurrenceWriterConverter() {
        throw new IllegalStateException("Utility class");
      }


    public static Document convert(RecurrencePublicationData source) {
        Document result = new Document();
        HoursAndMinutes  hoursAndMinutes = source.getHoursAndMinutes();
        if (hoursAndMinutes != null) {
            result.append("hoursAndMinutes", HoursAndMinutesWriterConverter.convert(hoursAndMinutes));
        }

        List<Integer>  daysOfWeek = source.getDaysOfWeek();
        if (daysOfWeek != null)
            result.append("daysOfWeek", daysOfWeek);

        List<Integer>  months = source.getMonths();
        if (months != null)
            result.append("months", months);

        String timeZone = source.getTimeZone();
        if ((timeZone == null)||timeZone.equals(""))
            timeZone = defaultTimeZone;
        result.append("timeZone", timeZone);

        Integer durationInMinutes = source.getDurationInMinutes();
        if (durationInMinutes != null) {
            result.append("durationInMinutes", durationInMinutes);
        }

        return result;
    }
}
