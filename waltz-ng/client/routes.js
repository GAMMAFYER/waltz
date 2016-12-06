// -- BASIC ROUTES ---

function configureRoutes($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.when('', '/home');

    $stateProvider
        .state('main', {
            url: '/',
            views: {
                'header': {template: '<waltz-navbar></waltz-navbar>'},
                'content': require('./welcome/welcome.js')
            }
        })
        .state('main.home', {
            url: 'home',
            views: {
                'content': {template: require('./welcome/welcome.html')}
            }
        });
}

configureRoutes.$inject = [
    '$stateProvider',
    '$urlRouterProvider'
];


// -- SCROLLER ---

function configureScrollToTopOnChange($rootScope, $doc) {
    $rootScope.$on('$stateChangeSuccess', () => {
        $doc[0].body.scrollTop = 0;
        $doc[0].documentElement.scrollTop = 0;
    });
}


configureScrollToTopOnChange.$inject = [
    '$rootScope',
    '$document'
];


// -- NAG ---

function configureBetaNagMessageNotification($rootScope,
                                             nagMessageService,
                                             notification) {

    const nagFunction = (message = "") => {
        $rootScope.$on('$stateChangeSuccess', () => {
            notification.info(message);
        });
    };

    nagMessageService.setupNag(nagFunction);
}


configureBetaNagMessageNotification.$inject = [
    '$rootScope',
    'NagMessageService',
    'Notification'
];


// -- STATE CHANGE ---

function configureStateChangeListener($rootScope, $window, accessLogStore) {
    $rootScope.$on(
        '$stateChangeSuccess',
        (event, toState, toParams /* fromState, fromParams */ ) => {
            const {name} = toState;
            const infoPromise = accessLogStore.write(name, toParams);

            if (__ENV__ === 'prod') {
                infoPromise.then(info => {
                    if (info.revision != __REVISION__) {
                        console.log(
                            'Waltz reloading as server reported version does not match client. Server:',
                            info,
                            "client: ",
                            __REVISION__);
                        $window.location.reload()
                    }
                })

            }
        }
    );
}


configureStateChangeListener.$inject = [
    '$rootScope',
    '$window',
    'AccessLogStore'
];


// -- INACTIVITY TIMER ---

function configureInactivityTimer($rootScope, $timeout, $window, settingsService) {
    const uiInactivityTimeoutSettingKey = 'ui.inactivity-timeout';

    const reloadPage = () => {
        console.log("Reloading page due to inactivity");
        $window.location.reload();
    };

    settingsService
        .findOrDefault(uiInactivityTimeoutSettingKey, null)
        .then(inactivityTime  => {

            if(inactivityTime) {
                console.log('Configuring inactivity timer for ' + inactivityTime + " ms");
                let timeoutPromise = $timeout(reloadPage, inactivityTime);

                $rootScope.$on(
                    '$stateChangeSuccess',
                    (event, toState, toParams /* fromState, fromParams */) => {

                        //if existing timeout promise, then cancel
                        if (timeoutPromise) {
                            $timeout.cancel(timeoutPromise);
                        };

                        // set a new countdown
                        timeoutPromise = $timeout(reloadPage, inactivityTime);
                    }
                );
            }

        });

}


configureInactivityTimer.$inject = [
    '$rootScope',
    '$timeout',
    '$window',
    'SettingsService'
];


// -- SETUP ---

function setup(module) {
    module
        .config(configureRoutes)
        .run(configureScrollToTopOnChange)
        .run(configureBetaNagMessageNotification)
        .run(configureStateChangeListener)
        .run(configureInactivityTimer);

}


export default setup;