

MxUnit  { 
	
	classvar registery,<protoHandler;

	var <>source,<inlets,<outlets,<>handlers;
	var <>group,status;
	
	*make { arg source,class;
		var handlers;
		if(source.isKindOf(MxUnit) or: {source.isNil},{
			^source
		});
		handlers = this.handlersFor(class ? source.class);
		^handlers.use {
			var unit;
			unit = ~make.value(source);
			if(~source.isNil,{
				~source = source;
			});
			unit.handlers = handlers;
			~unit = unit;
			unit
		}
	}
	saveData {
		var data,ids;
		data = this.use { ~save.value(source) };
		^[source.class.name,data]
	}
	*loadData { arg data;
		var source,class;
		# class, data = data;
		class = class.asClass;
		source = this.handlersFor(class).use { ~load.value(data) };
		^this.make(source,class)
	}
	*new { arg source,inlets,outlets;
		^super.newCopyArgs(source,inlets,outlets).init
	}
	init {
		inlets.do(_.unit = this);
		outlets.do(_.unit = this);
	}
	*handlersFor { arg class;
		var h;
		h = protoHandler.copy;
		class.superclassesDo({ arg class;
			var match,path;
			match = registery[class.name];
			if(match.isNil,{
				path = PathName(MxUnit.class.filenameSymbol.asString).parentPath 
								+/+ "drivers" +/+ class.name.asString ++ ".scd";
				if(File.exists(path),{
					path.load;
					match = registery[class.name]
				});
			});				
			if(match.notNil,{
				h.putAll(match);
				^h
			})
		});
		Error("No MxUnit driver found for " + class).throw;
	}
	getInlet { arg index;
		if(index.isNil,{
			^inlets.first
		});
		if(index.isInteger,{
			^inlets[index]
		},{
			inlets.do { arg in;
				if(in.name == index,{
					^in
				})
			}
		});
		Error("Inlet not found:" + index).throw
	}
	getOutlet { arg index;
		if(index.isNil,{
			^outlets.first
		});
		if(index.isInteger,{
			^outlets[index]
		},{
			outlets.do { arg in;
				if(in.name == index,{
					^in
				})
			}
		});
		Error("Outlet not found:" + index).throw
	}
	*register { arg classname,handlers;
		registery.put(classname.asSymbol, handlers)
	}

	isPrepared {
		^['isPrepared','isPlaying','isStopped'].includes(status)
	}
	// methods delegated to the handlers
	prepareToBundle { arg agroup, bundle, private, bus;
		^this.use({ 
			~prepareToBundle.value(agroup,bundle,true,bus); 
			bundle.addFunction({status='isPrepared'})
		})
	}
	spawnToBundle { arg bundle;
		^this.use { 
			~spawnToBundle.value(bundle);
			bundle.addFunction({ status = 'isPlaying' })
		}
	}
	stopToBundle { arg bundle;
		^this.use { 
			~stopToBundle.value(bundle);
			bundle.addFunction({ status = 'isStopped' })
		}
	}
	freeToBundle { arg bundle;
		^this.use { 
			~freeToBundle.value(bundle);
			bundle.addFunction({status='isFreed'})
		}
	}
	respawnToBundle { arg bundle;
		this.stopToBundle(bundle);
		this.spawnToBundle(bundle);
	}	
	moveToHead { arg aGroup,bundle;
		^this.use {
			~moveToHead.value(aGroup,bundle,group)
		}
	}
	
	use { arg function;
		var result, saveEnvir;

		saveEnvir = currentEnvironment;
		currentEnvironment = handlers;
		protect {
			result = function.value(handlers)
		} { arg exception;
			if(exception.notNil) {
				("MxUnit" + this.source + this.source.class + "ERROR in:\n" + function.def + "\n" + function.def.sourceCode).postln;
			};
			currentEnvironment = saveEnvir;
		};
		^result		
	}
	//delegate
	callHandler { arg method ... args;
		var result, saveEnvir;

		saveEnvir = currentEnvironment;
		currentEnvironment = handlers;
		protect {
			result = handlers[method].valueArray(args)
		} { arg exception;
			if(exception.notNil) {
				("MxUnit" + this.source + "ERROR in" + method + args).postln;
				// can fetch the handler source code here
			};
			currentEnvironment = saveEnvir;
		};
		^result
	}
	
	play { arg group,atTime,bus;
		^this.use { ~play.value(group,atTime,bus) }
	}
	stop { arg atTime,andFreeResources=true;
		^this.use { ~stop.value(atTime,andFreeResources) }
	}
	respawn { arg atTime;
		^this.use { ~respawn.value(atTime) }
	}		
	isPlaying {
		^this.use { ~isPlaying.value }
	}
	numChannels {
		^this.use { ~numChannels.value }
	}
	spec {
		^this.use { ~spec.value }
	}
	copySource {
		^this.use { ~copy.value }
	}
	// relocate  toBeat, atTime
	name {
		^this.use { ~name.value }
	}
	gui { arg layout,bounds;
		^this.use { ~gui.value(layout,bounds) }
	}	
	draw { arg pen,bounds,style;
		^this.use { ~draw.value(pen,bounds,style) }
	}
	timeGui { arg layout,bounds,maxTime;
		^this.use { ~timeGui.value(layout,bounds,maxTime) }
	}
	zoomTime { arg fromTime,toTime;
		^this.use { ~zoomTime.value(fromTime,toTime) }
	}
	gotoBeat { arg beat,atBeat,bundle;
		^this.use { ~gotoBeat.value(beat,atBeat,bundle) }
	}
	canRecord {
		^handlers['record'].notNil
	}
	record { arg boo=true,atTime;
		^this.use { ~record.value(boo,atTime) }
	}
	*initClass {
		registery = IdentityDictionary.new;
		
		protoHandler = (
			make: { arg object; MxUnit(object) },
			save: { ~source.asCompileString },
			load: { arg string; string.compile.value() },
			copy: { ~source.deepCopy },
			
			prepareToBundle:  { arg agroup, bundle, private, bus; ~source.prepareToBundle(agroup,bundle,private,bus) },
			spawnToBundle: { arg bundle; ~source.spawnToBundle(bundle) },
			stopToBundle: { arg bundle; ~source.stopToBundle(bundle) },
			freeToBundle: { arg bundle; ~source.freeToBundle(bundle) },
			moveToHead: { arg aGroup,bundle,currentGroup; 
				// default is to stop it and fully restart it
				// objects that can move themselves can implement this cleaner
				~stopToBundle.value(bundle);
				~prepareToBundle.value(aGroup,bundle);
				~spawnToBundle.value(bundle);
			},
			play: { arg group, atTime, bus;},
			stop: { arg atTime,andFreeResources = true; },
			respawn: { arg atTime; },
			isPlaying: { ~source.isPlaying },
			
			numChannels: { ~source.numChannels ? 2 },
			spec: { ~source.spec ?? {'audio'.asSpec} },
			gui: { arg layout,bounds; 
				~source.gui(layout ?? {Window(~name.value,bounds).front},bounds) 
			},
			draw: { arg pen,bounds,style;
				pen.color = style['fontColor'];
				pen.font = style['font'];
				if(style['center'],{
					pen.stringCenteredIn(~name.value,bounds)
				},{
					pen.stringLeftJustIn(~name.value, bounds.insetBy(2,2) )
				});
			}, 
			name: { ~source.asString }

			// crop
			// relocate: { arg toBeat, atTime; }
			// timeGui
			// zoomTimeGui
			// asCompileString
			// 
		);
	}
}



MxInlet {
	
	var <>name,<>index,<>spec,<>adapter;
	var <>unit;
	
	*new { arg name,index,spec,adapter;
		^super.newCopyArgs(name.asSymbol,index,spec.asSpec,adapter)
	}
	storeArgs {
		// adapter: AbsMxAdapter subclass
		// which is not really savable
		^[name,index,spec,adapter]
	}
	printOn { arg stream;
		stream << "in:" << name
	}
}


MxOutlet : MxInlet {

	printOn { arg stream;
		stream << "out:" << name
	}
}

