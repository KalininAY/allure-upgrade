'use strict';

//allure.api.addTab('resultiks', {
//    title: 'tab.packages.name', icon: 'fa fa-align-left',
//    route: 'packages(/)(:testGroup)(/)(:testResult)(/)(:testResultTab)(/)',
//    onEnter: (function (testGroup, testResult, testResultTab) {
//        return new allure.components.TreeLayout({
//            testGroup: testGroup,
//            testResult: testResult,
//            testResultTab: testResultTab,
//            tabName: 'tab.packages.name',
//            baseUrl: 'packages',
//            url: 'data/packages.json'
//        });
//    })
//});

setTimeout(function() {
	let color = new Date() % 2 ? 'red' : 'blue';
	document.querySelector('.chart__svg').style.backgroundColor = color
}, 1000);