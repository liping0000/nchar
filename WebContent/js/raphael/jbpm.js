
//http://www.irunmywebsite.com/raphael/additionalhelp.html?q=usingelementevents


(function () {

	Raphael.fn.jbpm = Raphael.fn.jbpm || {};

	Raphael.fn.jbpm.context = false;

	Raphael.fn.jbpm.shim = {stroke: "none", fill: "#000", "fill-opacity": 0};
	Raphael.fn.jbpm.txtattr = {font: "12px Arial, sans-serif"};
	// known workflow nodes
	Raphael.fn.jbpm.knownNodes = {
			"start":{color:"#0f0"},
			"customTaskActivity":{color:"#77f"},
			"automaticTaskActivity":{color:"#9f9"},
			"createBusinessKey":{color:"#9f9"},
			"end-cancel":{color:"#f00"},
			"end":{color:"#f55"}};
	// the wrapper div as jQuery object
	Raphael.fn.jbpm.canvasDiv = false;
	// the handler for mouse move event
	Raphael.fn.jbpm.mouseHandler = false;

	Raphael.fn.jbpm.xmlDoc = false;
	Raphael.fn.jbpm.vertices = false;

	// start for a new transition
	Raphael.fn.jbpm.startVertex = false;
	// current node at mouse position
	Raphael.fn.jbpm.currentVertex = false;
	// current edge at mouse position
	Raphael.fn.jbpm.currentEdge = false;
	// start for a new transition
	Raphael.fn.jbpm.currentLink = false;
	// any visible popup
	Raphael.fn.jbpm.currentPopup = false;
	// the edge or vertex for the popup, both should implement the unlink method
	Raphael.fn.jbpm.currentPopup.context = false;

	// actions called from the popup menus
	// deletes the current edge or node at the mouse position
	Raphael.fn.jbpm.deleteCurrent = function() {
		// remove the element from the xml doc and the UI
		Raphael.fn.jbpm.currentPopup.context.unlink();
		Raphael.fn.jbpm.currentPopup.context = null;
		// remove the popup
		Raphael.fn.jbpm.currentPopup.remove();
	};
	Raphael.fn.jbpm.renameCurrent = function() {
		var newName = "changerequest.newName";
		// remove the element from the xml doc and the UI
		Raphael.fn.jbpm.currentPopup.context.rename(newName);
		Raphael.fn.jbpm.currentPopup.remove();
	};
	Raphael.fn.jbpm.editCurrent = function() {	
		alert("edit: " + Raphael.fn.jbpm.currentPopup.context.xml.attribute("name"));
		Raphael.fn.jbpm.currentPopup.remove();
	};
	Raphael.fn.jbpm.createNode = function() {
		//alert("create node");
		// remove the popup
		Raphael.fn.jbpm.currentPopup.remove();

		var nodeName = "changerequest.newNode";
		var dim = "10,10,50,50";

		var newNodeElement = new REXML_XMLElement(
				"element",
				"customTaskActivity",
				" name=\"" + nodeName + "\""      // toUpperCase()
				+ " form=\"someFormHere.html\""   // required
				+ " g=\"" + dim + "\"/",
				Raphael.fn.jbpm.xmlDoc,      // parent (root)
				"");
		var newNodeUI = Raphael.fn.jbpm.vertex.apply(Raphael.fn.jbpm.context, [newNodeElement]);
		Raphael.fn.jbpm.vertices.put(nodeName, newNodeUI);
	};



	////////////////////////////////////////////////////
	//
	// the workflow object, we do a 2 pass parsing here,
	// first the nodes, then the edges...
	//
	//  the data is stored in a HashMap of sets (vertices) and the xml document (xmlDoc)
	//  each set contains the drawing parameters for a single node that are relevant for
	//  the view in addition to that each set has a link to the matching part of the xml document
	//  the xml document is updated on user interaction
	//
	//  workflowDefinition: the workflow in xml/jPDL
	//  canvasDiv: the enclosing div needed for position calculations
	//
	//
	Raphael.fn.jbpm.workflow = function(workflowDefinition, canvasDiv) {
		// the raphael context
		Raphael.fn.jbpm.context = this;

		// disable default context menu if we are in an edge or node
		jQuery(document)[0].oncontextmenu = function() {
			if (Raphael.fn.jbpm.currentVertex || Raphael.fn.jbpm.currentEdge) {
				// no browser popup on edges or vertices
				return false;
			}
		};
		canvasDiv.bind('contextmenu', function() { return false; });

		Raphael.fn.jbpm.vertices = new HashMap();

		// parsing the workflow definition, see: http://www.levelthreesolutions.com/jsxml/documentation.html
		Raphael.fn.jbpm.xmlDoc = new REXML(workflowDefinition);
		Raphael.fn.jbpm.canvasDiv = canvasDiv; // use offset.left, offset.top for the coords of the parent div


		// function to create the workflow code
		this.code = function() {
			code = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
			code += "<process name=\"" + Raphael.fn.jbpm.xmlDoc.rootElement.attribute("name") + "\" >\n";
			code += ElementCode(Raphael.fn.jbpm.vertices.values(), 2);
			code += "</process>\n";
			return code;
		};

		// walking through the child elements of the process definition
		for (var i=0; i<Raphael.fn.jbpm.xmlDoc.rootElement.childElements.length; i++) {
			var child = Raphael.fn.jbpm.xmlDoc.rootElement.childElements[i];
			// the tagname is in child.name
			if (child.name in this.jbpm.knownNodes) {
				// var dim = child.attribute("g").split(",");
				// var name = child.attribute("name");
				// oThis.jbpm.vertex is a function that returns a set object
				Raphael.fn.jbpm.vertices.put(child.attribute("name"), this.jbpm.vertex(child));
			}
		}


		// now create the edges, when an edge is created it registers itself
		// in the start and end vertex
		for (var i=0; i<Raphael.fn.jbpm.xmlDoc.rootElement.childElements.length; i++) {
			var startNode = Raphael.fn.jbpm.xmlDoc.rootElement.childElements[i];
			// the tagname is in child.name
			if (startNode.name in Raphael.fn.jbpm.knownNodes) {
				var startNodeName = startNode.attribute("name"); // this is the attribute name not the tagname
				// walk the child elements of the node an look for transitions
				for (var j=0; j<startNode.childElements.length; j++) {
					var transition = startNode.childElements[j];
					if (transition.name == "transition") {
						var endNodeName = transition.attribute("to");
						var transitionName = transition.attribute("name");
						this.jbpm.edge(
								Raphael.fn.jbpm.vertices.get(startNodeName),
								Raphael.fn.jbpm.vertices.get(endNodeName),
								transitionName, "#78f");
					}
				} // end analyzing transition
			} // end known node
		} // end walking child nodes


		// register mouse move listener on the canvas
		//this.canvas.moveHandler = false;

		// registering on the canvas is too slow in chrome:
		//    this.canvas.onmousemove = function (e) {
		// so we use the parent div to track any mouse movements
		canvasDiv.mousemove(function (e) {
// doesn't work in chrome:
//			jQuery('#log').replaceWith('<div id=log>'
//					+ e.clientX + "," + e.clientY
//					+ (Raphael.fn.jbpm.mouseHandler?':handler':':noHandler')
//					+ (Raphael.fn.jbpm.currentVertex?(' current:'+ Raphael.fn.jbpm.currentVertex.name):':noCurrentVertex')
//					+ (Raphael.fn.jbpm.startVertex?(' start:'+ Raphael.fn.jbpm.startVertex.name):':noStartVertex')
//					+ '</div>');
			var current = Raphael.fn.jbpm.currentVertex;
			// if we have a drag handler call it with the new position
			if (Raphael.fn.jbpm.mouseHandler) {
				var pos = EventPosition(e);
				// callback to the drag Handler registered by the element
				Raphael.fn.jbpm.mouseHandler.move(pos.x, pos.y);
				// safari redraw fix
				Raphael.fn.jbpm.context.safari();
			}
			return false;  // same as .preventDefault() and .stopPropagation(), speeds things up
		});
		canvasDiv.mouseup(function (e) {
			e = e || window.event;
			if (e.button == 2) {
				e.preventDefault();
				e.stopPropagation();
				//alert("mousedown");
			}
			if (Raphael.fn.jbpm.mouseHandler) {
				var pos = EventPosition(e);
				Raphael.fn.jbpm.mouseHandler.up(pos.x, pos.y);
			}
			return false; // same as .preventDefault() and .stopPropagation(), speeds things up
		});
		canvasDiv.mousedown(function (e) {
			var pos = EventPosition(e);
			e = e || window.event;
			if (e.button == 2) {
				e.preventDefault();
				e.stopPropagation();
				var popup = Raphael.fn.jbpm.currentPopup;
				if (popup) {
					popup.hide();
					// FIXME: dispose the popup
					Raphael.fn.jbpm.currentPopup = false;
				}
				// display: none; position: fixed; top: " + pos.y + "px; left: " + pos.x +
				var offset = Raphael.fn.jbpm.canvasDiv.offset();
				var windowX = pos.x + Math.round(offset.left);
				var windowY = pos.y + Math.round(offset.top);

				popup = jQuery(
					"<div class='rich-menu-list-bg' style='position:absolute;left:" + windowX + "px;top:" + windowY + "px;'>"
				  + "<div class='rich-menu-item rich-ddmenu-label'>"
				  + "<span class='rich-menu-item-icon'>"
				  + "</span>"
				  + "<span class='rich-menu-item-label'>"
				  + "<a href='JavaScript:Raphael.fn.jbpm.createNode();'>create</a>"
			      + "</span>"
				  + "</div>");

				Raphael.fn.jbpm.canvasDiv.prepend(popup);
				Raphael.fn.jbpm.currentPopup = popup;
			}
		});

		return this;
	};






	////////////////////////////////////////////////////
	//
	// this is a factory method that return a vertex for the jbpm graph
	// which is a set of vector objects (rect, label)
	//
	Raphael.fn.jbpm.vertex = function(element) {
		var oThis = this;

		// create the set of graph elements for this object,
		// the set is the object we return from this function
		var UI = this.set();
		// for storing the incoming and outgoing edges, these arrays are filled
		// in the function creating the edges
		UI.outgoing = [];
		UI.incoming = [];
		// remember the xml element
		UI.xml = element;


		var dim = UI.xml.attribute("g").split(",");
		var labelText = UI.xml.attribute("name").split(".")[1];
		var x = parseInt(dim[0]);
		var y = parseInt(dim[1]);
		var width = parseInt(dim[2]);
		var height = parseInt(dim[3]);

		var cornerRadius = 5;
		var fixOpacity = ".7";
		var moveOpacity = ".5";
		var borderOpacity = ".9";


		//return this.rect(x, y, /*width*/ 60, /*height*/ 40, /*cornerRadius*/ 5);
		var rect = this.rect(x, y, width, height, /*cornerRadius*/ 5).attr({opacity: fixOpacity});
		//var color = Raphael.getColor(); // returns a random color
		var color = Raphael.fn.jbpm.knownNodes[UI.xml.name].color;
		rect.attr({fill: color, stroke: color, "fill-opacity": fixOpacity, "stroke-width": 1});
		UI.push(rect);

		var text = this.text(x + width/2, y + height/2, labelText).attr(this.jbpm.txtattr).attr({stroke: "#223", "stroke-width": 0.1});
		UI.push(text);


		// the move action, we need to move all elements in the set
		// this function is the callback for the drag handler


		// this generates a line from the rect to the mouse position
		//var


		// register a drag listener on mousedown for the set
		// note that this might be either the text or the rect
		UI.mousedown(function (e) {
			var pos = EventPosition(e);
			e = e || window.event;
			// see: http://www.quirksmode.org/js/events_properties.html#button
			if (e.button == 2) {
				e.preventDefault();
				e.stopPropagation();
				var popup = Raphael.fn.jbpm.currentPopup;
				if (popup) {
					popup.hide();
					// FIXME: dispose the popup
					Raphael.fn.jbpm.currentPopup = false;
				}
				// display: none; position: fixed; top: " + pos.y + "px; left: " + pos.x +
				var offset = Raphael.fn.jbpm.canvasDiv.offset();
				var windowX = pos.x + Math.round(offset.left);
				var windowY = pos.y + Math.round(offset.top);

				popup = jQuery(
					"<div class='rich-menu-list-bg' style='position:absolute;left:" + windowX + "px;top:" + windowY + "px;'>"
				  + "<div class='rich-menu-item rich-ddmenu-label'>"
				  + "<span class='rich-menu-item-label'>"
				  + "<a href='JavaScript:Raphael.fn.jbpm.deleteCurrent();'>delete</a>"
			      + "</span>"
			      + "</div>"
				  + "<div class='rich-menu-item rich-ddmenu-label'>"
				  + "<span class='rich-menu-item-label'>"
				  + "<a href='JavaScript:Raphael.fn.jbpm.renameCurrent();'>rename</a>"
			      + "</span>"
			      + "</div>"
				  + "<div class='rich-menu-item rich-ddmenu-label'>"
				  + "<span class='rich-menu-item-label'>"
				  + "<a href='JavaScript:Raphael.fn.jbpm.editCurrent();'>edit</a>"
			      + "</span>"
			      + "</div>"
				  + "</div>");

				Raphael.fn.jbpm.canvasDiv.prepend(popup);
				Raphael.fn.jbpm.currentPopup = popup;
				Raphael.fn.jbpm.currentPopup.context = UI;
			}
			///////// common stuff

			// get position inside the canvas
			var pos = EventPosition(e);
			e.preventDefault && e.preventDefault();
			// set the click position right away
			this.dx = pos.x;
			this.dy = pos.y;
			// this function itself becomes the drag handler for the canvas
			Raphael.fn.jbpm.mouseHandler = this;
			Raphael.fn.jbpm.startVertex = UI;


			// check if the click was at the border
			if (IsBorderClick(pos, UI)) {  // border click, start linking
				// start pull animation
				rect.animate({"fill-opacity": borderOpacity}, 250);
				// init a new link in Raphael, this is picked up in the linkSet method
				Raphael.fn.jbpm.currentLink = oThis.path();
				// callback for dragging used by the workflow canvas when a drag is detected
				this.move = function(newX, newY) {
					if (!Raphael.fn.jbpm.currentLink) {
						return; // link might already be removed
					}
					var endPoint = Raphael.fn.jbpm.currentVertex;
					// calculate the endpoint, a fake bbox
					if (!endPoint) {
						endPoint = {};
						endPoint.x = newX;
						endPoint.y = newY;
						endPoint.width = 10;
						endPoint.height = 10;
					}
					var link = CalculatePath(UI, endPoint);

					// do the drawing by updating the path
					Raphael.fn.jbpm.currentLink.attr({path:link.path, stroke: "#78f", "stroke-width": 2, fill: "none", color: "#78f"});
				};
				this.up = function(newX, newY) {
					// end of link action, we might need to setup a new transition
					if (Raphael.fn.jbpm.startVertex && Raphael.fn.jbpm.currentVertex) {
						var start = Raphael.fn.jbpm.startVertex;
						var end = Raphael.fn.jbpm.currentVertex;

						// name of the target
						var linkName = end.xml.attribute("name").split(".")[1];
						// reate the name of the transition
						linkName = "to" + linkName.substr(0, 1).toUpperCase() + linkName.substr(1);
						// calling in the scope of the oThis function:
						Raphael.fn.jbpm.edge.apply(oThis, [start, end, linkName, "#78f"]);
						// we have to add a transition child element to the xml document node of the start element:
						start.xml.childElements[start.xml.childElements.length] = new REXML_XMLElement(
								"element",
								"transition",
								" name=\"" + linkName + "\""  //toUpperCase()
								+ " to=\"" + end.xml.attribute("name") + "\"/",
								end.xml,      // parent (this node)
								"");
					}
					Raphael.fn.jbpm.currentLink.remove();
					Raphael.fn.jbpm.currentLink = false;
					Raphael.fn.jbpm.startVertex = false;
					Raphael.fn.jbpm.currentVertex = false;
					Raphael.fn.jbpm.mouseHandler = false;
				};
			} else {   // not a border click, start dragging
				// start drag animation
				rect.animate({"fill-opacity": moveOpacity}, 250);
				// callback for dragging used by the workflow canvas when a drag is detected
				this.move = function(newX, newY) {
					// doing the drag stuff with deltas
					var xPos = newX - Raphael.fn.jbpm.mouseHandler.dx;
					var yPos = newY - Raphael.fn.jbpm.mouseHandler.dy;

					for (var i = 0; i < UI.length; i++) {
						UI[i].translate(xPos,yPos);
					}
					// repaint the edges
					for (var i = 0; i < UI.outgoing.length; i++) {
						UI.outgoing[i].redraw();
					}
					for (var i = 0; i < UI.incoming.length; i++) {
						UI.incoming[i].redraw();
					}

					// updating the click position on a mouse move event (caller triggered)
					Raphael.fn.jbpm.mouseHandler.dx = newX;
					Raphael.fn.jbpm.mouseHandler.dy = newY;
				};
				this.up = function(newX, newY) {
					// end of drag action
					rect.animate({"fill-opacity": fixOpacity}, 250);
					var newLocation = Math.floor(UI[0].attrs.x) + "," +
					Math.floor(UI[0].attrs.y) + "," +
					Math.floor(UI[0].attrs.width) + "," +
					Math.floor(UI[0].attrs.height);
					// update the xml element
					UI.xml.attribute("g", newLocation);
					Raphael.fn.jbpm.mouseHandler = false;
				};
			}
		});

		// manage the current vertex property
		UI.mouseover(function (e) {
			Raphael.fn.jbpm.currentVertex = UI;
			e.preventDefault && e.preventDefault();
		});
		UI.mouseout(function (e) {
			Raphael.fn.jbpm.currentVertex = false;
			e.preventDefault && e.preventDefault();
		});


		UI.unlinkStart = function(transitionName) {
			// remove the edge from the internal array
			for (var i = 0; i < this.outgoing.length; i++) {
				if (this.outgoing[i].label == transitionName) {
					this.outgoing.splice(i,1);
				}
			}
			// remove the edge from the underlaying xml doc,
			// its a part of the start nodes children
			var children = this.xml.childElements;
			for (var i = 0; i < children.length; i++) {
				if (children[i].name == "transition") {
					if (children[i].attribute("name") == transitionName) {
						children.splice(i,1);
					}
				}
			}
		};
		UI.unlinkEnd = function(transitionName) {
			// remove the edge from the internal array
			for (var i = 0; i < this.incoming.length; i++) {
				if (this.incoming[i].label == transitionName) {
					this.incoming.splice(i,1);
				}
			}
		};
		UI.unlink = function() {
			// unlink edges
			// FIXME: concurrent modification of the outgoing/incoming array here
			for (var i = 0; i < this.incoming.length; i++) {
				this.incoming[i].startVertex.unlinkStart(this.incoming[i].label);
				this.incoming[i].remove();
			}
			for (var i = 0; i < this.outgoing.length; i++) {
				this.outgoing[i].remove();
			}
			// remove from the array of vertices
			Raphael.fn.jbpm.vertices.pop(UI.xml.attribute("name"));
			// remove from the UI
			UI.remove();
		};

		UI.rename = function(newName) {
			// remove from the array of vertices
			var nodeUI = Raphael.fn.jbpm.vertices.pop(UI.xml.attribute("name"));
			nodeUI.xml.attribute("name", newName);
			// add to the hashmap again
			Raphael.fn.jbpm.vertices.put(nodeUI.xml.attribute("name"), nodeUI);
		}

		return UI;
	};


	////////////////////////////////////////////////////
	//
	// an edge for the jbpm graph
	//
	//  start ----------> end
	//
	Raphael.fn.jbpm.edge = function (start, end, labelText, stroke) {
		var oThis = this;

		// we just skip the loops and anything invalid
		if ( !end || !start || (start == end)) {
			return;
		}

		// create the set to hold the information for this edge
		var UI = this.set();
		// register the edge in the nodes
		start.outgoing.push(UI);
		end.incoming.push(UI);

		UI.startVertex = start;
		UI.endVertex = end;
		UI.label = labelText;

		var pathData = CalculatePath(start, end);

		// this does the drawing
		var edge = this.path(pathData.path).attr({stroke: stroke, "stroke-width": 2, fill: "none"});
		var text = this.text(
				pathData.labelX,
				pathData.labelY,
				labelText).attr(this.jbpm.txtattr).attr({stroke: "#223", "stroke-width": 0.1});

		UI.push(edge);  // [0]
		UI.push(text);  // [1]

		// the context menu trigger for the edges
		UI.mousedown(function (e) {
			var pos = EventPosition(e);
			e = e || window.event;
			// see: http://www.quirksmode.org/js/events_properties.html#button
			if (e.button == 2) {
				e.preventDefault();
				e.stopPropagation();
				var popup = Raphael.fn.jbpm.currentPopup;
				if (popup) {
					popup.hide();
					// FIXME: dispose the popup
					Raphael.fn.jbpm.currentPopup = false;
				}
				// display: none; position: fixed; top: " + pos.y + "px; left: " + pos.x +
				var offset = Raphael.fn.jbpm.canvasDiv.offset();
				var windowX = pos.x + Math.round(offset.left);
				var windowY = pos.y + Math.round(offset.top);

				popup = jQuery(
					"<div class='rich-menu-list-bg' style='position:absolute;left:" + windowX + "px;top:" + windowY + "px;'>"
				  + "<div class='rich-menu-item rich-ddmenu-label'>"
				  + "<span class='rich-menu-item-icon'>"
				  + "</span>"
				  + "<span class='rich-menu-item-label'>"
				  + "<a href='JavaScript:Raphael.fn.jbpm.deleteCurrent();'>delete</a>"
			      + "</span>"
				  + "</div>");

				Raphael.fn.jbpm.canvasDiv.prepend(popup);
				Raphael.fn.jbpm.currentPopup = popup;
				Raphael.fn.jbpm.currentPopup.context = UI;
			}
		});
		// manage the current edge property
		UI.mouseover(function (e) {
			Raphael.fn.jbpm.currentEdge = UI;
			e.preventDefault && e.preventDefault();
		});
		UI.mouseout(function (e) {
			Raphael.fn.jbpm.currentEdge = false;
			e.preventDefault && e.preventDefault();
		});



		// redraws the edge by recalculating the path and label position and pushing the data in the set
		UI.redraw = function() {
			var pathData = CalculatePath(UI.startVertex, UI.endVertex);
			// first element is the line, we have to update the path string
			// in order to get it repainted with different data:
			UI[0].attr({path:pathData.path, stroke: stroke, "stroke-width": 2, fill: "none"});
			// moving the text to the new position:
			UI[1].attr({x: pathData.labelX, y: pathData.labelY});
		};

		UI.unlink = function() {
			UI.startVertex.unlinkStart(UI.label);
			UI.endVertex.unlinkEnd(UI.label);
			// remove from the UI
			UI.remove();
		};

		return UI;
	}; // end Raphael.fn.jbpm.edge





	/**
	 * a generic hashmap implementation from
	 * http://www.bennadel.com/blog/1399-Ask-Ben-Iterating-Over-An-Array-With-jQuery.htm
	 * for mapping the vertices names to the vertices objects for easy edge generation
	 */
	var HashMap = function() {
		var indexes = [];
		var values = [];

		//objHash.set('indexName', 123)
		this.put = function(name, vertex) {
			var locationIndex = indexes.indexOf(name);
			if (locationIndex==-1) {
				locationIndex = indexes.length;
				indexes[locationIndex] = name;
			} else {
				// putting the same name in the hash
				// again is a programmers error
			  alert("name already used");
			}
			values[locationIndex] = vertex;
		};

		this.pop = function(name) {
			var pos = indexes.indexOf(name);
			var result = values[pos];
			indexes.splice(pos, 1);
			values.splice(pos, 1);
			return result;
		};

		this.values = function() {
			return values;
		};

		//objHash.get('indexName')
		this.get = function(name) {
			return values[indexes.indexOf(name)];
		};

		//objHash.getByNumber('indexName')
		this.getByNumber = function(number) {
			return values[number];
		};

		//objHash.size
		this.size = function() {
			return indexes.length;
		};

		//objHash.each(function(value))
		this.each = function(callback){
			for (var i=0;i<indexes.length;i++){
				// callback(indexes[i], values[i]);
				callback(values[i]);
			}
		};
	};



	// clientX and clientY are relative to the viewport
	// this method calculates the event coordinates inside the document
	// this means eleminating any scoll offsets
	// and substracts the enclosing divs position giving us the click position
	// inside the canvas...
	// see: http://www.quirksmode.org/js/events_properties.html#position
	var EventPosition = function(e) {
		var divOff = Raphael.fn.jbpm.canvasDiv.offset();
		// if (!e) var e = window.event;
		// or:
		e = e || window.event;
		// calculate the new position
		var newX = 0;
		var newY = 0;
		if (e.pageX || e.pageY) {
			newX = e.pageX - divOff.left;
			newY = e.pageY - divOff.top;
		} else if (e.clientX || e.clientY) 	{
			newX = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft - divOff.left;
			newY = e.clientY + document.body.scrollTop + document.documentElement.scrollTop - divOff.top;
		}
		return { x: Math.floor(newX), y: Math.floor(newY)};
	};

	var IsBorderClick = function(pos, set) {
		var t = 7;  // border thinkness. seems like this only counts inside the set
		var box = set.getBBox();
		if ( ( (box.x - t < pos.x) && (pos.x < box.x + t) )                             // left border
				|| ( (box.y - t < pos.y) && (pos.y < box.y + t) )                           // top border
				|| ( (box.x + box.width - t < pos.x) && (pos.x < box.x + box.width + t) )   // right border
				|| ( (box.y + box.height - t < pos.y) && (pos.y < box.y + box.height + t) ) // bottom border
		) {
			return true;
		}
		return false;
	};



	// create a string for the workflow element
	// elements: array of set elements or UI vertices
	var ElementCode = function(elements, nesting) {

		// alert("element count: "+ elements.length);

		// some blanks to have a bit of pretty printing
		var blanks = "\n";
		for (var i=0; i < nesting; i++) {
			blanks += " ";
		}
		var code = "";
		for (var i=0;i<elements.length;i++){
			// the element might be a set for the UI which contains an xml element,
			// we want that xml instead of the UI
			var element = elements[i];
			if (typeof(element.xml) != "undefined") {
				element = element.xml;
			}


			switch (element.name) {
			case 'text':
				//var subelements = ElementCode(element.childElements, nesting + 2);
				var text = "";
				if (element.childElements[0].type == "cdata") {
					text += "<![CDATA[";
					text += element.childElements[0].text
					text += "]]>";
				}
				//alert("subelements of text: " + element.childElements);
				code += blanks
				+ "<text>"
				+ text
				+ "</text>";
				break;
			case 'script':
				var subelements = ElementCode(element.childElements, nesting + 2);
				code += blanks
				+ "<script"
				+ " lang=\"" + element.attribute("lang") + "\">"
				+ subelements
				+ blanks + "</script>";
				break;
			case 'on':
				var subelements = ElementCode(element.childElements, nesting + 2);
				code += blanks
				+ "<on"
				+ " event=\"" + element.attribute("event") + "\">"
				+ subelements
				+ blanks + "</on>";
				break;
			case 'customMail':
				code += blanks
				+ "<customMail"
				+ " templateName=\"" + element.attribute("templateName") + "\""
				+ " />";
				break;
			case 'transition':
				code += blanks
				+ "<transition"
				+ " name=\"" + element.attribute("name") + "\""
				+ " to=\"" + element.attribute("to") + "\""
				+ " />";
				break;
			case 'customTaskActivity':
				var subelements = ElementCode(element.childElements, nesting + 2);
				
				var spawnSignals = element.attribute("spawnSignals");
				var termSignals = element.attribute("termSignals");
				var groupActorName = element.attribute("groupActorName");
				
				if (spawnSignals) {
					spawnSignals = " spawnSignals=\"" + spawnSignals + "\"";
				}
				if (termSignals) {
					termSignals = " termSignals=\"" + termSignals + "\"";
				}
				if (groupActorName) {
					groupActorName = " groupActorName=\"" + groupActorName + "\"";
				}

				
				code += blanks
				+ "<customTaskActivity"
				+ " name=\"" + element.attribute("name") + "\""
				+ " form=\"" + element.attribute("form") + "\""  // form is required
				+ spawnSignals
				+ termSignals
				+ groupActorName
				+ " g=\"" + element.attribute("g") + "\""
				+ ">"
				+ subelements
				+ blanks + "</customTaskActivity>\n";
				break;
			case 'automaticTaskActivity':
				var subelements = ElementCode(element.childElements, nesting + 2);

				code += blanks
				+ "<automaticTaskActivity"
				+ " name=\"" + element.attribute("name") + "\""
				+ " g=\"" + element.attribute("g") + "\""
				+ ">"
				+ subelements
				+ blanks + "</automaticTaskActivity>\n";
				break;
			case 'start':
				var subelements = ElementCode(element.childElements, nesting + 2);
				code += blanks + "<start"
				+ " name=\"" + element.attribute("name") + "\""
				+ " form=\"" + element.attribute("form") + "\""
				+ " g=\"" + element.attribute("g") + "\""
				+ ">"
				+ subelements
				+ blanks + "</start>\n";
				break;
			case 'createBusinessKey':
				var subelements = ElementCode(element.childElements, nesting + 2);
				code += blanks + "<createBusinessKey"
				+ " name=\"" + element.attribute("name") + "\""
				+ " g=\"" + element.attribute("g") + "\""
				+ " prefix=\"" + element.attribute("prefix") + "\""
				+ " location=\"" + element.attribute("location") + "\""
				+ ">"
				+ subelements
				+ blanks + "</createBusinessKey>\n";
				break;
			case 'end':
				code += blanks + "<end"
				+ " name=\"" + element.attribute("name") + "\""
				+ " g=\"" + element.attribute("g") + "\""
				+ ">"
				+ blanks + "</" + element.name + ">\n";
				break;
			case 'end-cancel':
				code += blanks + "<end-cancel"
				+ " name=\"" + element.attribute("name") + "\""
				+ " g=\"" + element.attribute("g") + "\""
				+ ">"
				+ blanks + "</end-cancel>\n";
				break;
			default:
				// filter out comments:
				if (typeof(element.element) != "undefined") {
					code += blanks + "<" + element.name + "/>";
				}
			}
		} // end for loop over elements
		return code;
	};

	// The SVG-default origin is in the upper left. The x-axis increases
	// from left-to-right. The y-axis increases from top-to-bottom.
	// see: http://fastsvg.com/notes/axis.html
	// a path is a string with draw commands which are performed by SVG
	var CalculatePath = function(obj1, obj2) {
		var l = 11;  // size of the arrow
		var w = 3;

			// calculate the distance for the 4 different sides of the 2 rectangles
			// we want to connect
			var bb1 = obj1.getBBox();
			var bb2 = obj2.getBBox?obj2.getBBox():obj2;

			// calculate the end-points
			var p = [{x: bb1.x + bb1.width / 2, y: bb1.y - 1 },              // top middle of obj1    [0]
			         {x: bb1.x + bb1.width / 2, y: bb1.y + bb1.height + 1 }, // bottom middle of obj1 [1]
			         {x: bb1.x - 1 , y: bb1.y + bb1.height / 2},             // left middle of obj1   [2]
			         {x: bb1.x + bb1.width + 1 , y: bb1.y + bb1.height / 2}, // right middle of obj1  [3]

			         {x: bb2.x + bb2.width / 2, y: bb2.y - 1 - l},              // top middle of obj2    [4]
			         {x: bb2.x + bb2.width / 2, y: bb2.y + bb2.height + 1 + l}, // bottom middle of obj2 [5]
			         {x: bb2.x - 1 - l, y: bb2.y + bb2.height / 2},             // left middle of obj2   [6]
			         {x: bb2.x + bb2.width + 1 + l, y: bb2.y + bb2.height / 2}];// right middle of obj2  [7]

			var d = {};    // object to remember the points for the deltas
			var dis = [];  // array for the deltas
			for (var i = 0; i < 4; i++) {                // loop through obj1's endpoints
				for (var j = 4; j < 8; j++) {            // loop through obj2's endpoints
					// calculate the deltas
					var dx = Math.abs(p[i].x - p[j].x);
					var dy = Math.abs(p[i].y - p[j].y);
					var delta = Math.round(Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2)));
					dis.push(delta);
					// i: start indes, j: end index
					d[delta] = [i, j]; // this syntax adds a new property to the d object:
				}
			}
			if (dis.length == 0) {   // we found no deltas, just use default points: middle top
				var res = [0, 4];
			} else {
				// apply the min function to the dis array and get the
				// points for the min dis from the d object
				// returns an array wih the two indices for the points we want:
				var res = d[Math.min.apply(Math, dis)];
				// res[0] is the index of the start point in p [0..3]
				// res[1] is the index of the end point in p [4..7]
			}

			// calculate the line for the min distance we found
			var x1 = p[res[0]].x;
			var y1 = p[res[0]].y;
			var x4 = p[res[1]].x;
			var y4 = p[res[1]].y;
			var dx = Math.max(Math.abs(x1 - x4) / 2, l);   // Bezier control point distance from the start/end
			var dy = Math.max(Math.abs(y1 - y4) / 2, l);

			// calculating the first control point
			var x2 = [x1, x1, x1 - dx, x1 + dx][res[0]].toFixed(3);
			var y2 = [y1 - dy, y1 + dy, y1, y1][res[0]].toFixed(3);

			// calculating the second control point
			var x3 = [x4, x4, x4 - dx, x4 + dx][res[1] - 4].toFixed(3);
			var y3 = [y1 + dy, y1 - dy, y4, y4][res[1] - 4].toFixed(3);

			////// calculating a triangle for the end of the curve (the arrow)
			// we start with the end-point of the curve plus the arrow length which is the tip of the arrow
			var x5 = [x4, x4, x4 + l, x4 - l][res[1] - 4].toFixed(3);
			var y5 = [y4 + l, y4 - l, y4, y4][res[1] - 4].toFixed(3);
			// right side of the arrow (in arrow direction) : top, bottom, left, right
			var x6 = [x4 - w, x4 + w, x4, x4][res[1] - 4].toFixed(3);
			var y6 = [y4, y4, y4 + w, y4 - w][res[1] - 4].toFixed(3);
			// left side of the arrow (in arrow direction) : top, bottom, left, right
			var x7 = [x4 + w, x4 - w, x4, x4][res[1] - 4].toFixed(3);
			var y7 = [y4, y4, y4 - w, y4 + w][res[1] - 4].toFixed(3);

			// turning an array into a string:
			// see: http://www.w3.org/TR/SVG/paths.html#PathData for the format
			var path = ["M",                                    // move to absolute position
			            x1.toFixed(3), y1.toFixed(3),           // start point
			            "C",
			            x2, y2, x3, y3,                         // control points for the Bezier Curve
			            x4.toFixed(3), y4.toFixed(3),
			            "M",                                    // start the arrow pointer
			            x5, y5,
			            x6, y6,
			            x7, y7,
			            "Z"
			            ].join(",");// end point

			// calculate a result object
			var result = {};
			result.path = path;
			result.labelX = Math.round((x1 + x4) / 2.0);
			result.labelY = Math.round((y1 + y4) / 2.0);

			return result;
		}; // end getPathString


})();
