/**
 * 
 */

var taxoname = ["arts and entertainment", "education", "science"];

var subtaxoname1 = ["books and literature", "music"]; //arts and entertainment
var subtaxoname2 = ["language learning", "special education", "homeschooling"]; //education
var subtaxoname3 = []; //health and fitness
var subtaxoname4 = ["biology", "ecology", "mathematics"]; //science

var cat11 = ["children books(5)"]; //books and literature
var cat12 = []; //dance
var cat13 = ["country music(1)", "classical music(2)"]; //music

var cat21 = ["english(7)", "italian(2)"]; //language learning

var cat31 = []; //disease
var cat32 = []; //therapy

var cat41 = []; //biology
var cat42 = ["pollution(3)", "waste management(1)"]; //ecology
var cat43 = ["algebra(1)", "geometry(2)", "statistics(1)"]; //mathematics


function prepareCategoryInterface() {
	
	// manipulate UI elements using jquery custom ui
	
	initTreeDiagram();
	
};

function initTreeDiagram() {
	var treedata = prepareTreeData();
	drawTreeDiagram(treedata);
}

function prepareTreeData() {
	
	// use global variable uname and uniqueCats
	var tdata = [];
	var parent = {};
	// add total number of bookmarks e.g. Chloe(100) as node name
	//parent.name = uname + "(" + initbookmarks.length + ")";
	parent.name = "YELL(50)";
	parent.parent = "null";
	// children1 - taxonomy array
	var children1 = [];
	for (var i = 0; i < taxoname.length; i++) {
		var child1 = {};
		// get the total count as well (need a new function)
		child1.parent = "Top Level";
		child1.name = taxoname[i]
		// children 2 - subtoxonomies
		var children2 = getSubtoxonomies(child1.name);
		if (children2.length > 0) {
			child1.children = children2;
		}
		children1.push(child1);
	}
	parent.children = children1; 

	tdata.push(parent);
	return tdata;
}

function getSubtoxonomies(childname) {
	var children2 = [];
	if (childname == "arts and entertainment"){
		subs = subtaxoname1;
	} else if (childname == "education"){
		subs = subtaxoname2;
	} else if (childname == "health and fitness"){
		subs = subtaxoname3;
	} else if (childname == "science"){
		subs = subtaxoname4;
	} else {
		subs = [];
	}
	
	for (var i = 0; i < subs.length; i++) {
			if (subs[i] != "x") {
				var child2 = {};
				child2.parent = childname;
				child2.name = subs[i];
				// children3 - categories
				var children3 = getCategories(child2.name);
				if (children3.length > 0) {
					child2.children = children3;
				}
				children2.push(child2);
			}
	}

	return children2;
}

function getCategories(subtoxname) {
	var children3 = [];
	if (subtoxname == "books and literature"){
		cats = cat11;
	} else if (subtoxname == "dance"){
		cats = cat12;
	} else if (subtoxname == "music"){
		cats = cat13;
	} else if (subtoxname == "language learning"){
		cats = cat21;
	} else if (subtoxname == "disease"){
		cats = cat31;
	} else if (subtoxname == "therapy"){
		cats = cat32;
	} else if (subtoxname == "biology"){
		cats = cat41;
	} else if (subtoxname == "ecology"){
		cats = cat42;
	} else if (subtoxname == "mathematics"){
		cats = cat43;
	} else {
		cats = [];
	}
	
	for (var i = 0; i < cats.length; i++) {
			if (cats[i] != "x") {
				var child3 = {};
				child3.name = cats[i];
				child3.parent = subtoxname;
				children3.push(child3);
			}
		
	}
	
	return children3;
}

function drawTreeDiagram(treeData) {

	// ************** Generate the tree diagram *****************
	var margin = {
		top : 20,
		right : 120,
		bottom : 20,
		left : 120
	}, width = 960 - margin.right - margin.left, height = 500 - margin.top
			- margin.bottom;

	var i = 0, duration = 750, root;

	var tree = d3.layout.tree().size([ height, width ]);

	var diagonal = d3.svg.diagonal().projection(function(d) {
		return [ d.y, d.x ];
	});

	var bycat = document.getElementById('cattree');
	var svg = d3.select(bycat).append("svg").attr("width",
			width + margin.right + margin.left).attr("height",
			height + margin.top + margin.bottom).append("g").attr("transform",
			"translate(" + margin.left + "," + margin.top + ")");

	root = treeData[0];
	root.x0 = height / 2;
	root.y0 = 0;

	update(root);

	d3.select(self.frameElement).style("height", "500px");

	function update(source) {

		// Compute the new tree layout.
		var nodes = tree.nodes(root).reverse(), links = tree.links(nodes);

		// Normalize for fixed-depth.
		nodes.forEach(function(d) {
			d.y = d.depth * 180;
		});

		// Update the nodes…
		var node = svg.selectAll("g.node").data(nodes, function(d) {
			return d.id || (d.id = ++i);
		});

		// Enter any new nodes at the parent's previous position.
		var nodeEnter = node.enter().append("g").attr("class", "node").attr(
				"transform", function(d) {
					return "translate(" + source.y0 + "," + source.x0 + ")";
				}).on("click", click).on("contextmenu", rightclick);
		nodeEnter.attr("id", "test");
		nodeEnter.append("circle").attr("r", 1e-6).style("fill", function(d) {
			return d._children ? "lightsteelblue" : "#fff";
		});

		nodeEnter.append("text").attr("x", function(d) {
			return d.children || d._children ? -13 : 13;
		}).attr("dy", ".35em").attr("text-anchor", function(d) {
			return d.children || d._children ? "end" : "start";
		}).text(function(d) {
			return d.name;
		}).style("fill-opacity", 1e-6);

		// nodeEnter.on("contextmenu", rightclick);

		// Transition nodes to their new position.
		var nodeUpdate = node.transition().duration(duration).attr("transform",
				function(d) {
					return "translate(" + d.y + "," + d.x + ")";
				});

		nodeUpdate.select("circle").attr("r", 10).style("fill", function(d) {
			return d._children ? "lightsteelblue" : "#fff";
		});

		nodeUpdate.select("text").style("fill-opacity", 1);

		// Transition exiting nodes to the parent's new position.
		var nodeExit = node.exit().transition().duration(duration).attr(
				"transform", function(d) {
					return "translate(" + source.y + "," + source.x + ")";
				}).remove();

		nodeExit.select("circle").attr("r", 1e-6);

		nodeExit.select("text").style("fill-opacity", 1e-6);

		// Update the links…
		var link = svg.selectAll("path.link").data(links, function(d) {
			return d.target.id;
		});

		// Enter any new links at the parent's previous position.
		link.enter().insert("path", "g").attr("class", "link").attr("d",
				function(d) {
					var o = {
						x : source.x0,
						y : source.y0
					};
					return diagonal({
						source : o,
						target : o
					});
				});

		// Transition links to their new position.
		link.transition().duration(duration).attr("d", diagonal);

		// Transition exiting nodes to the parent's new position.
		link.exit().transition().duration(duration).attr("d", function(d) {
			var o = {
				x : source.x,
				y : source.y
			};
			return diagonal({
				source : o,
				target : o
			});
		}).remove();

		// Stash the old positions for transition.
		nodes.forEach(function(d) {
			d.x0 = d.x;
			d.y0 = d.y;
		});
	}

	// Toggle children on click.
	function click(d) {
		if (d.children) {
			d._children = d.children;
			d.children = null;
		} else {
			d.children = d._children;
			d._children = null;
		}
		update(d);
	}

	function rightclick(d) {
		//configureDialogCategory(d.name, d.depth);
		d3.event.preventDefault();
	}
}


