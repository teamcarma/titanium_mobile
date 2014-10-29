Appcelerator Titanium Mobile 3.2.1.Alpha
============================

Build
-----------

```

scons

```


Installation
-----------

```

ti sdk install https://github.com/teamcarma/titanium_mobile/releases/download/3_2_1_Alpha/mobilesdk-3.2.1.Alpha-osx.zip

```

**Fix ios-sim**

If having any problem with running ios-sim after the installation:

```

# Reinstall ios-sim
sudo npm uninstall -g ios-sim;
sudo npm install -g ios-sim;

# Change ownership of ios-sim;
sudo chown $USER /usr/local/lib/node_modules/ios-sim/build/Release/ios-sim;
sudo rm /usr/local/bin/ios-sim;
ln /usr/local/lib/node_modules/ios-sim/build/Release/ios-sim /usr/local/bin/ios-sim;

# Link Titanium ios-sim to the latest version
rm "/Users/$USER/Library/Application Support/Titanium/mobilesdk/osx/3.2.1.Alpha/iphone/ios-sim";
ln -s "/usr/local/bin/ios-sim" "/Users/$USER/Library/Application Support/Titanium/mobilesdk/osx/3.2.1.Alpha/iphone/ios-sim";


```


