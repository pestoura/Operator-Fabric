
describe ('Logging screen tests',function () {

    before('Set up configuration', function () {
        cy.loadTestConf();
    });

    it('Check composition of multi-filters for process groups/processes/states for operator1', function () {
        cy.loginOpFab('operator1', 'test');

        // We move to logging screen
        cy.get('#opfab-navbar-menu-logging').click();

        // We check we have 2 items in process groups multi-filter
        cy.get('#opfab-processGroup').click();
        cy.get('#opfab-processGroup').find('li').should('have.length', 2);
        cy.get('#opfab-processGroup').contains('Base Examples').should('exist');
        cy.get('#opfab-processGroup').contains('User card examples').should('exist');
        // We select all process groups
        cy.get('#opfab-processGroup').contains('Select All').click();

        // We check we have 4 items in process multi-filter
        cy.get('#opfab-process').click();
        cy.get('#opfab-process').find('li').should('have.length', 4);
        cy.get('#opfab-process').contains('Examples for new cards').should('exist');
        cy.get('#opfab-process').contains('Examples for new cards 2').should('exist');
        cy.get('#opfab-process').contains('IGCC').should('exist');
        cy.get('#opfab-process').contains('Process example').should('exist');

        // We select all processes
        cy.get('#opfab-process').contains('Select All').click();
        cy.get('#opfab-process').click();

        // We check we have 18 states (and 4 items for their process)
        cy.get('#opfab-state').click();
        cy.get('#opfab-state').find('li').should('have.length', 22);

        // We unselect all processes then we select 'Process example' process and we check there are 8 states for this process
        cy.get('#opfab-process').click();
        cy.get('#opfab-process').contains('UnSelect All').click();
        cy.get('#opfab-process').contains('Process example').click();
        cy.get('#opfab-process').click();
        cy.get('#opfab-state').click();
        cy.get('#opfab-state').find('li').should('have.length', 9);
        // We check this state is present because it is only a child state but that kind of state must be visible in logging screen
        cy.get('#opfab-state').contains('Planned outage date response').should('exist');
    })

    it('Check composition of multi-filters for process groups/processes/states for operator4', function () {
        cy.loginOpFab('operator4', 'test');

        // We move to logging screen
        cy.get('#opfab-navbar-menu-logging').click();

        // We check we have 2 items in process groups multi-filter
        cy.get('#opfab-processGroup').click();
        cy.get('#opfab-processGroup').find('li').should('have.length', 2);
        cy.get('#opfab-processGroup').contains('Base Examples').should('exist');
        cy.get('#opfab-processGroup').contains('User card examples').should('exist');
        // We select all process groups
        cy.get('#opfab-processGroup').contains('Select All').click();

        // We check we have 4 items in process multi-filter
        cy.get('#opfab-process').click();
        cy.get('#opfab-process').find('li').should('have.length', 4);
        cy.get('#opfab-process').contains('Examples for new cards').should('exist');
        cy.get('#opfab-process').contains('Examples for new cards 2').should('exist');
        cy.get('#opfab-process').contains('IGCC').should('exist');
        cy.get('#opfab-process').contains('Process example').should('exist');

        // We select all processes
        cy.get('#opfab-process').contains('Select All').click();
        cy.get('#opfab-process').click();

        // We check we have 11 states (and 4 items for their process)
        cy.get('#opfab-state').click();
        cy.get('#opfab-state').find('li').should('have.length', 15);

        // We unselect all processes then we select 'Process example' process and we check there is only 1 state for this process
        cy.get('#opfab-process').click();
        cy.get('#opfab-process').contains('UnSelect All').click();
        cy.get('#opfab-process').contains('Process example').click();
        cy.get('#opfab-process').click();
        cy.get('#opfab-state').click();
        cy.get('#opfab-state').find('li').should('have.length', 2);
        cy.get('#opfab-state').contains('Action Required', {matchCase: false}).should('exist');
    })

    it('Check composition of multi-filters for process groups/processes/states for admin', function () {
        cy.loginOpFab('admin', 'test');

        // We move to logging screen
        cy.get('#opfab-navbar-menu-logging').click();

        // We check the 3 multi-filters for service/process/state do not exist
        cy.get('#opfab-processGroup').should('not.exist');
        cy.get('#opfab-process').should('not.exist');
        cy.get('#opfab-state').should('not.exist');

        cy.get('#opfab-logging-no-process-state-available').should('exist');
        cy.get('#opfab-logging-no-process-state-available').contains('No process/state available').should('exist');
    })

    it('Check composition of multi-filters for process groups/processes/states for operator1, with a config without process group', function () {
        cy.loginOpFab('operator1', 'test');

        cy.loadEmptyProcessGroups();
        cy.reload();

        // We move to logging screen
        cy.get('#opfab-navbar-menu-logging').click();

        // We check process groups multi-filter do not exist
        cy.get('#opfab-processGroup').should('not.exist');

        // We check we have 4 items in process multi-filter
        cy.get('#opfab-process').click();
        cy.get('#opfab-process').find('li').should('have.length', 4);
        cy.get('#opfab-process').contains('Examples for new cards').should('exist');
        cy.get('#opfab-process').contains('Examples for new cards 2').should('exist');
        cy.get('#opfab-process').contains('IGCC').should('exist');
        cy.get('#opfab-process').contains('Process example').should('exist');

        // We select all processes
        cy.get('#opfab-process').contains('Select All').click();
        cy.get('#opfab-process').click();

        // We check we have 18 states (and 4 items for their process)
        cy.get('#opfab-state').click();
        cy.get('#opfab-state').find('li').should('have.length', 22);

        // We unselect all processes then we select 'Process example' process and we check there are 8 states for this process
        cy.get('#opfab-process').click();
        cy.get('#opfab-process').contains('UnSelect All').click();
        cy.get('#opfab-process').contains('Process example').click();
        cy.get('#opfab-process').click();
        cy.get('#opfab-state').click();
        cy.get('#opfab-state').find('li').should('have.length', 9);
        // We check this state is present because it is only a child state but that kind of state must be visible in logging screen
        cy.get('#opfab-state').contains('Planned outage date response').should('exist');

        cy.loadTestConf();
        cy.reload();
    })
})