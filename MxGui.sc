

MxGui : AbstractPlayerGui {
	
	var boxes;

	writeName {}	

	guiBody { arg layout,bounds;
		var bb;
		bounds = bounds ?? {layout.innerBounds.moveTo(0,0)};
		bb = bounds.resizeBy(-200,0);
		boxes = MxMatrixGui(model, layout, bb );
		this.drawer(layout,(bounds - bb).resizeTo(200,bounds.height));
	}

	drawer { arg layout,bounds;
		var d;
		d = MxDrawer({ arg obj;
			var unit;
			if(boxes.focusedPoint.notNil,{
				unit = model.put(boxes.focusedPoint.x,boxes.focusedPoint.y,obj);
				boxes.refresh;
				model.update;
			})
		});
		d.gui(layout,bounds)
	}
}


