

MxDrawer.addGroup("Instr",{
	Instr.leaves.collect { arg instr; [instr.dotNotation ,instr] }
},{ arg instr,i,instrname,onMake;
	onMake.value(instr)
});

MxDrawer.add("Slider",{ arg i,onMake; onMake.value(NumberEditor.new) });

MxDrawer.add("CC",{ arg i,onMake; onMake.vavlue(CCResponder.new) });
