var exec = require('cordova/exec');

var safePluginExport = {};

safePluginExport.read = function (filter, success, failure) {
    exec(success, failure, 'ReadSMS', 'read', [filter]);
};

safePluginExport.requestPermissionIfNeed = function (success, failure) {
    exec(success, failure, 'ReadSMS', 'requestPermissionIfNeed', []);
};

module.exports = safePluginExport;