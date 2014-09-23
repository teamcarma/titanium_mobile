/*
 * run.js: Titanium iOS CLI run hook
 *
 * Copyright (c) 2012-2013, Appcelerator, Inc.  All Rights Reserved.
 * See the LICENSE file for more information.
 */

var appc = require('node-appc'),
	__ = appc.i18n(__dirname).__,
	afs = appc.fs,
	fs = require('fs'),
	path = require('path'),
	parallel = require('async').parallel,
	cp = require('child_process'),
	exec = cp.exec,
	spawn = cp.spawn;

exports.cliVersion = '>=3.2';

exports.init = function (logger, config, cli) {

	cli.addHook('build.post.compile', {
		priority: 10000,
		post: function (build, finished) {
			if (cli.argv.target != 'simulator') return finished();

			if (cli.argv['build-only']) {
				logger.info(__('Performed build only, skipping running of the application'));
				return finished();
			}

			/**
			 *
			 * Get the logs from the simulator code...
			 * 
			 * @param  {Function} callback
			 * @return
			 */
			var getLogs = function(callback){

				var simulatorDir = afs.resolvePath('~/Library/Developer/CoreSimulator/Devices/');

				if(!afs.exists(simulatorDir)){
					simulatorDir = afs.resolvePath('~/Library/Application Support/iPhone Simulator/' + build.iosSimVersion +
						(appc.version.gte(build.iosSimVersion, '7.0.0') && cli.argv['sim-64bit'] ? '-64' : '') + '/Applications');
				}

				exec('find ' + simulatorDir + ' -iname "*.log"', function(error, stdout, stderr){
					var logs = [];
					stdout.split("\n")
						.forEach(function(log){ 
							if(log && !log.match(/\/tmp/)) logs.push(log); 
						});
					callback(logs);
				});
					
			};

			logger.info(__('Running application in iOS Simulator'));

			var simulatorDir = afs.resolvePath('~/Library/Application Support/iPhone Simulator/' + build.iosSimVersion +
					(appc.version.gte(build.iosSimVersion, '7.0.0') && cli.argv['sim-64bit'] ? '-64' : '') + '/Applications'),
				logFile = build.tiapp.guid + '.log';

			parallel([
				function (next) {
					logger.debug(__('Terminating all iOS simulators'));
					exec('/usr/bin/killall ios-sim', setTimeout(next, 250));
				},

				function (next) {
					exec('/usr/bin/killall "iPhone Simulator"', setTimeout(next, 250));
				},

				function (next) {
					// sometimes the simulator doesn't remove old log files in which case we get
					// our logging jacked - we need to remove them before running the simulator
					afs.exists(simulatorDir) && fs.readdirSync(simulatorDir).forEach(function (guid) {
						var file = path.join(simulatorDir, guid, 'Documents', logFile);
						if (afs.exists(file)) {
							logger.debug(__('Removing old log file: %s', file.cyan));
							fs.unlinkSync(file);
						}
					});

					setTimeout(next, 250);
				},

				function (next) {
					/// erase the logs
					getLogs(function(files){
						files.forEach(function(file){ 
							if (afs.exists(file)) fs.unlinkSync(file);
						});
						setTimeout(next, 250);
					});
				},

				
			], function () {

				var cmd = [
						'"' + path.join(build.titaniumIosSdkPath, 'ios-sim') + '"',
						'launch',
						'"' + build.xcodeAppDir + '"',
						'--sdk',
						appc.version.format(build.iosSimVersion, 2, 2),
						'--family',
						build.iosSimType
					],
					findLogTimer,
					simProcess,
					simErr = [],
					stripLogLevelRE = new RegExp('\\[(?:' + logger.getLevels().join('|') + ')\\] '),
					simStarted = false,
					simEnv = path.join(build.xcodeEnv.path, 'Platforms', 'iPhoneSimulator.platform', 'Developer', 'Library', 'PrivateFrameworks') +
							':' + afs.resolvePath(build.xcodeEnv.path, '..', 'OtherFrameworks');

				if (cli.argv.retina) {
					cmd.push('--retina');
					if (appc.version.gte(build.iosSimVersion, '6.0.0') && build.iosSimType == 'iphone' && cli.argv.tall) {
						cmd.push('--tall');
					}
				}
				if (appc.version.gte(build.iosSimVersion, '7.0.0') && cli.argv['sim-64bit']) {
					cmd.push('--sim-64bit');
				}
				cmd = cmd.join(' ');

				logger.info(__('Launching application in iOS Simulator'));
				logger.trace(__('Simulator environment: %s', ('DYLD_FRAMEWORK_PATH=' + simEnv).cyan));
				logger.debug(__('Simulator command: %s', cmd.cyan));

				simProcess = spawn('/bin/sh', ['-c', cmd], {
					cwd: build.titaniumIosSdkPath,
					env: {
						DYLD_FRAMEWORK_PATH: simEnv
					}
				});

				simProcess.stderr.on('data', function (data) {
					logger.debug(data);
					data.toString().split('\n').forEach(function (line) {
						line.length && simErr.push(line.replace(stripLogLevelRE, ''));
					}, this);
				}.bind(this));

				simProcess.on('exit', function (code, signal) {
					return;
					clearTimeout(findLogTimer);

					if (simStarted) {
						var endLogTxt = __('End simulator log');
						logger.log(('-- ' + endLogTxt + ' ' + (new Array(75 - endLogTxt.length)).join('-')).grey);
					}

					if (code || simErr.length) {
						finished(new appc.exception(__('An error occurred running the iOS Simulator'), simErr));
					} else {
						logger.info(__('Application has exited from iOS Simulator'));
						finished();
					}

				}.bind(this));

				// focus the simulator
				logger.info(__('Focusing the iOS Simulator'));
				exec([
					'osascript',
					'"' + path.join(build.titaniumIosSdkPath, 'iphone_sim_activate.scpt') + '"',
					'"' + path.join(build.xcodeEnv.path, 'Platforms', 'iPhoneSimulator.platform', 'Developer', 'Applications', 'iPhone Simulator.app') + '"'
				].join(' '), function (err, stdout, stderr) {
					if (err) {
						logger.error(__('Failed to focus the iPhone Simulator window'));
						logger.error(stderr);
					}
				});

				var levels = logger.getLevels(),
					logLevelRE = new RegExp('^(\u001b\\[\\d+m)?\\[?(' + levels.join('|') + '|log|timestamp)\\]?\s*(\u001b\\[\\d+m)?(.*)', 'i');

				function findLogFile() {

					getLogs(function(files){

						var file,
							l = files.length,
							positions = {};

						/// sync issues
						if(!files.length){
							setTimeout(findLogFile, 250);
							return;
						}

						for (var i=0; i < files.length; i++) {

							file = files[i];
							
							// if we found the log file, then the simulator must be running
							simStarted = true;

							// pipe the log file
							logger.debug(__('-----------------------------'));
							logger.debug(__('Found iPhone Simulator log file: %s', file.cyan));
							logger.debug(__('-----------------------------'));

							var startLogTxt = __('Start simulator log');
							logger.log(('-- ' + startLogTxt + ' ' + (new Array(75 - startLogTxt.length)).join('-')).grey);
							
							var readChangesTimer = null;

							(function readChanges () {
								
								try {
									var buf = new Buffer(16),
										buffer = '',
										lastLogger = 'debug';

									var stats = fs.statSync(file),
										fd, bytesRead, lines, m,line, len;

									if(!positions[file]){
										positions[file] = 0;
									}

									if (positions[file] < stats.size) {
										fd = fs.openSync(file, 'r');
										do {
											bytesRead = fs.readSync(fd, buf, 0, 16, positions[file]);
											positions[file] += bytesRead;
											buffer += buf.toString('utf-8', 0, bytesRead);
										} while (bytesRead === 16);
										fs.closeSync(fd);

										lines = buffer.split('\n');
										buffer = lines.pop(); // keep the last line because it could be incomplete
										for (var i = 0, len = lines.length; i < len; i++) {
											line = lines[i];
											if (line) {
												m = line.match(logLevelRE);
												if (m) {
													lastLogger = m[2].toLowerCase();
													line = m[4].trim();
												}
												if (levels.indexOf(lastLogger) == -1) {
													logger.log(('[' + lastLogger.toUpperCase() + '] ').cyan + line);
												} else {
													logger[lastLogger](line);
												}
											}
										}
									}
									
								} catch (ex) {
									
									// if (ex.code == 'ENOENT') {
									// 	clearTimeout(readChangesTimer);
									// 	if (simStarted) {
									// 		var endLogTxt = __('End simulator log');
									// 		logger.log(('-- ' + endLogTxt + ' ' + (new Array(75 - endLogTxt.length)).join('-')).grey);
									// 	}
									// 	logger.log();
									// 	process.exit(0);
									// }
									// throw ex;
									// 
								}
								finally {
									readChangesTimer = setTimeout(readChanges, 50);
								}

							}());

							simProcess.on('exit', function() {
							 	clearTimeout(readChangesTimer);
							});

						}

					});

					// didn't find any log files, try again in 250ms
					//findLogTimer = setTimeout(findLogFile, 250);
				}

				setTimeout(findLogFile, 1000);

			});
		}
	});

};
