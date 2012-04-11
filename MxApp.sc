

AbsApp {

	var <model,mxapp;
	
	*new { arg model,mxapp;
		^super.newCopyArgs(model,mxapp).prInit
	}
	prInit {}
	mx { ^mxapp.model }
}


MxApp : AbsApp {
	
	var cache,convertor;

	mx { ^model }
	at { arg point;
		^this.prFind( model.at(point.x,point.y) )
	}
	channel { arg i;
		var c;
		c = model.channelAt(i) ?? {
				model.extendChannels(i);
				model.update;
				model.channelAt(i)
			};
		^this.prFind( c )
	}
	master {
		^this.prFind( model.master )
	}
	
	newChan {
		^this.channel( model.channels.size )
	}
	add { arg ... sources;
		// returns a channel filled with object(s)
		var chan;
		chan = this.prFind( model.add(*sources) );
		model.update;
		this.mx.changed('grid');
		^chan
	}
	
	play { arg then;
		if(model.isPlaying.not,{
			model.onPlay(then).play
		},then)
	}
	stop { arg then;
		if(model.isPlaying,{
			model.onFree(then).free
		},then)
	}

	relocate { arg beat,q=4;
		model.gotoBeat(beat,q);
	}
	
	save {
		model.save
	}
	gui { arg layout,bounds;
		// detect and front
		var open;
		open = model.dependants.detect(_.isKindOf(MxGui));
		if(open.notNil,{
			open.front
		},{
			model.gui(layout,bounds);
		})
	}
	
	//select
	
	//copy to an app buffer
	//paste
	prInit {
		cache = IdentityDictionary.new;
	}
	prFind { arg obj;
		var app;
		^cache[obj] ?? {
			app = (obj.class.name.asString ++ "App").asSymbol.asClass.new(obj,this);
			cache[obj] = app;
			app
		}
	}
}


MxChannelApp : AbsApp {
	
	at { arg i;
		var unit;
		unit = model.at(i) ?? {
			^nil
			// slot is still nil
			//model.extendUnits(i);
			//model.at(i)
		};
		^mxapp.prFind( unit )
	}
	put { arg i,source;
		this.mx.put( this.channelNumber, i, source );
		this.mx.update;
		this.mx.changed('grid');
		^this.at(i)
	}
	removeAt { arg i;
		this.mx.remove( this.channelNumber, i );
		this.mx.changed('grid');
		this.mx.update
	}
	insertAt { arg i,source;
		// source.asArray.do
		this.mx.insert( this.channelNumber, i, source );
		this.mx.update;
		this.mx.changed('grid');
		^this.at(i)
	}
	
	//unit
	
	//select
	add { arg ... sources; // add 1 or more to the end
		var start,apps,ci;
		start = model.units.size;
		ci = this.channelNumber;
		apps = sources.collect { arg source,i;
			this.mx.put( ci,start + i, source );
			this.at(i)
		};
		this.mx.update;
		this.mx.changed('grid');
		if(apps.size == 1,{
			^apps.first
		},{
			^apps
		})
	}
	dup { arg fromIndex,toIndex;
		// toIndex defaults to the next slot, pushing any others further down		
		var unit,ci;
		ci = this.channelNumber;
		unit = this.mx.copy( ci, fromIndex, ci, 	toIndex ?? {fromIndex + 1} );
		this.mx.update;
		this.mx.changed('grid');
		if(unit.isNil, { ^nil });
		^mxapp.prFind( unit )
	}
	mute {
		this.mx.mute(this.channelNumber,true);
		this.mx.changed('mixer');
	}
	unmute {
		this.mx.mute(this.channelNumber,false);
		this.mx.changed('mixer');
	}
	toggle {
		this.mx.mute(this.channelNumber,model.fader.mute.not);
		this.mx.changed('mixer');
	}
	solo {
		this.mx.solo(this.channelNumber,true);
		this.mx.changed('mixer');
	}
	unsolo {
		this.mx.solo(this.channelNumber,false);
		this.mx.changed('mixer');
	}
	db {
		^model.fader.db
	}
	db_ { arg db;
		model.fader.db = db;
		this.mx.changed('mixer');
	}
	//fade { arg db,seconds=5; // easing
		// will need a little engine
	//}
	
	channelNumber {
		^this.mx.indexOfChannel(model)
	}
}


MxUnitApp : AbsApp {
	
	name {
		^model.name
	}
	source {
		^model.source
	}
	use { arg function;
		^model.use(function)
	}
	stop {
		model.stop;
		// unit should send state change notifications
		this.mx.changed('grid');
	}
	play {
		model.play;
		this.mx.changed('grid');
	}
	respawn {
		model.respawn
	}
	isPlaying {
		^model.isPlaying
	}
	spec {
		^model.spec
	}
	beatDuration {
		model.beatDuration
	}
	gui { arg layout,bounds;
		^model.gui(layout,bounds)
	}
	
	remove {
		this.mx.remove(*this.point.asArray).update;
		this.mx.changed('grid');
		// should mark self as dead
	}
	moveTo { arg point;
		var me;
		me = this.point;
		this.mx.move(me.x,me.y,point.x,point.y).update;
		this.mx.changed('grid');
	}
	//replaceWith { arg source; // or unit or point
	//}
	//replace(other)
	
	inlets {
		^model.inlets.collect(mxapp.prFind(_))
	}
	outlets {
		^model.outlets.collect(mxapp.prFind(_))
	}
	inlet { arg i; // index or name
		^this.prFindIOlet(i,model.inlets)
	}
	outlet { arg i;
		^this.prFindIOlet(i,model.outlet)
	}
	channel {
		^mxapp.prFind( this.mx.channelAt( this.point.x ) )
	}
	
	point { ^this.mx.pointForUnit(model) }
	
	prFindIOlet { arg i,iolets;
		if(i.isNumber,{
			// post error if out of bounds
			if(i >= model.inlets.size,{
				^nil
			},{
				^mxapp.prFind(model.inlets.at(i))
			})
		},{
			i = i.asSymbol;
			model.inlets.do { arg inl;
				if(inl.name == i,{
					^mxapp.prFind(inl)
				})
			}
		});
		^nil
	}
}


MxInletApp : AbsApp {

	>> { arg outlet; // wrong, backwards
		this.mx.connect(model.unit,model,outlet.unit,outlet);
		this.mx.update;
		this.mx.changed('grid');
		^outlet
	}
	<< { arg inlet;
		inlet -> this;
		^this
	}
	disconnect {
		this.mx.disconnectInlet(model);
		this.mx.update;
		this.mx.changed('grid');
	}
	spec {
		^model.spec
	}
	name {
		^model.name
	}
	index {
		^model.index
	}
	unit {
		^mxapp.prFind(model.unit)
	}
	from { // outlets that connect to me
		^this.mx.cables.toInlet(model).collect { arg cable;
			mxapp.prFind( cable.outlet )
		}
	}
	// cables
}


MxOutletApp : AbsApp {

	>> { arg inlet;
		this.mx.connect(model.unit,model,inlet.unit,inlet);
		this.mx.update;
		this.mx.changed('grid');
		^inlet // or magically find the outlet of that unit; or return that unit
	}
	<< { arg inlet;
		inlet -> this;
		^this
	}
	disconnect {
		this.mx.disconnectOutlet(model);
		this.mx.update;
		this.mx.changed('grid');
	}
	
	spec {
		^model.spec
	}
	name {
		^model.name
	}
	index {
		^model.index
	}
	unit {
		^mxapp.prFind(model.unit)
	}
	to { // outlets that connect to me
		^this.mx.cables.fromInlet(model).collect { arg cable;
			mxapp.prFind( cable.inlet )
		}
	}
}


MxQs {
	
	
}


	