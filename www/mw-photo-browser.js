//
// MWPhotoBrowser.js
//
// Created by Calvin Lai on  2013-08-16.
// Copyright 2013 Calvin Lai. All rights reserved.

var cordova = require('cordova'),
    exec = require('cordova/exec');

var PhotoBrowserPlugin = function() {
  // constructor
};

// Call this to register for push notifications and retreive a deviceToken
PhotoBrowserPlugin.prototype.showGallery = function(images, callback) {
  cordova.exec(callback, callback, "PhotoBrowserPlugin", "showGallery", images ? [images] : []);
};

PhotoBrowserPlugin.prototype.showBrowser = function(images, callback) {
  cordova.exec(callback, callback, "PhotoBrowserPlugin", "showBrowser", images ? [images] : []);
};


var photoBrowserPlugin = new PhotoBrowserPlugin();

module.exports = photoBrowserPlugin;
