SCMProxy {
	var name;
	var parentGroup;
	var < proxySpaceName;
	var <> serverGroup;
	var fadeIn;
	var fadeOut;
	var confirmFadeout;

	var < outputBus;
	var < channels;

	*new{
		arg proxyName, function, parent, audioIn, channels = 2;
		^super.new.init(proxyName, function, parent, audioIn, channels);
	}

	init{
		arg proxyName, function, parent, audioIn, channelsCount;

		name = proxyName;
		parentGroup = parent;

		channels = channelsCount;

		proxySpaceName = parentGroup.name ++ "_" ++ name;
		proxySpaceName = proxySpaceName.asSymbol;

		serverGroup = Group(parent.serverGroup, 'addToHead');//add to head in group, so it comes before groupFX

		outputBus = Bus.audio(Server.local, channels);

		fadeIn = 0;
		fadeOut = 2;
		confirmFadeout = false;

		SCM.proxySpace[proxySpaceName] = NodeProxy.audio(Server.local, channels);
		SCM.proxySpace[proxySpaceName].awake = false;//silently set proxy

		//if audio input is present, add it to the proxy and filter it, function's first input then becomes input
		(audioIn != nil).if
		({
			SCM.proxySpace[proxySpaceName][0] = audioIn;//add audio input
			SCM.proxySpace[proxySpaceName][1] = \filter -> function;//add filter function
		},
		{
			//otherwise just simple output
			SCM.proxySpace[proxySpaceName][0] = function;
		});

		SCM.proxySpace[proxySpaceName].end;//have to end, otherwise doesn't want to play... weird

		this.mapNodeProxyControls();

	}

	setInput{

	}


	mapNodeProxyControls{
		(SCM.proxySpace[proxySpaceName].isKindOf(NodeProxy)).if{
			var pairs;

			//get the pairs of ctrlnames and values from the node function
			(SCM.proxySpace[proxySpaceName].source.isKindOf(Function)).if{
				pairs = SCM.proxySpace[proxySpaceName].getKeysValues;
			};

			//if there a pairs to map
			pairs.isNil.not.if{
				//loop through pairs
				pairs.do{
					arg item, i;
					var name, value, postFix = "", ctrl, proxyCtrlName;
					//store name
					proxyCtrlName = item[0];
					proxyCtrlName = proxyCtrlName.asSymbol;

					name = item[0];
					value = item[1];

					//split into postfix
					name = name.asString.split($_);
					if(name.size > 1)
					{
						postFix = "/" ++ name[1];
					};
					if([\wet1, \wet2, \wet3, \wet4].includes(name[0].asSymbol) != true)
					{
						//create a control and store reference to nodeproxy and proxy control
						ctrl = parentGroup.newCtrl(name[0].asSymbol, value, postFix.asSymbol);
						ctrl.setupProxyControl(proxySpaceName, proxyCtrlName)
					}
				};
			};

		}
	}

	play{
		confirmFadeout = false;
		SCM.proxySpace[proxySpaceName].awake = true;
		SCM.proxySpace[proxySpaceName].resume;
		SCM.proxySpace[proxySpaceName].play(out: outputBus, group:serverGroup, addAction: 'addToTail', fadeTime: fadeIn);//if output//NodeProxy;

	}

	stop{
		SCM.proxySpace[proxySpaceName].stop(fadeOut);
		confirmFadeout = true;
		{ if(confirmFadeout){SCM.proxySpace[proxySpaceName].pause; }; }.defer(fadeOut);

	}

	getNodeProxy{
		^SCM.proxySpace[proxySpaceName];
	}

	getOutput{
		^In.ar(this.outputBus, channels);
	}

	listen{
		this.outputBus.play;
	}


}