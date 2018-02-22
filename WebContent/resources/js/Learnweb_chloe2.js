/**
 * Author: Chloe 
 */

var uniqueCats = [];
var uniqueSubs = [];
var uniqueTops = [];
var groupName;
var resourceTotal;


function prepareCatInterface(groupCatJson, gName, rTotal) {
	var cattree = JSON.parse(groupCatJson);
	uniqueCats = cattree.uniqueBots;
	uniqueSubs = cattree.uniqueMids;
	uniqueTops = cattree.uniqueTops;
	groupName = gName;
	resourceTotal = rTotal;

	initTreeDiagram();
}

function initTreeDiagram() {
	var treedata = prepareTreeData();
	drawTreeDiagram(treedata);
}

function prepareTreeData() {
	
	// use global variable uname and uniqueCats
	var tdata = [];
	var parent = {};
	
	parent.name = "ALL("+resourceTotal+")";
	parent.parent = "null";
	
	// children1 - top categories
	var children1 = [];
	for (var i = 0; i < uniqueTops.length; i++) {
		var child1 = {};
		child1.parent = "Top Level";
		child1.name = uniqueTops[i].cattop_name;
		
		// children 2 - middle categories
		var children2 = getMiddleCats(uniqueTops[i].id, child1.name);
		if (children2.length > 0) {
			child1.children = children2;
		}
		children1.push(child1);
	}
	parent.children = children1; 

	tdata.push(parent);
	
	return tdata;
}

function getMiddleCats(childid, childname) {
	var children2 = [];
	//iterate uniqueSubs and filter those whose top category matches childname
	for (var i = 0; i < uniqueSubs.length; i++) {
		
		if (uniqueSubs[i].cattop_id == childid){
			var child2 = {};
			child2.parent = childname;
			child2.name = uniqueSubs[i].catmid_name;
			// children3 - categories
			var children3 = getBottomCats(uniqueSubs[i].id, child2.name);
			if (children3.length > 0) {
				child2.children = children3;
			}
			children2.push(child2);
		}
	}
	
	return children2;
}

function getBottomCats(subid, subtoxname) {
	var children3 = [];
	//iterate through uniqueCats and filter those whose middle cat is same as subtoxname
	for (var i = 0; i < uniqueCats.length; i++) {
		
		if (uniqueCats[i].catmid_id == subid){
			var child3 = {};
			child3.name = uniqueCats[i].catbot_name;
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
	function rightclick(d) {
		if (d.children) {
			d._children = d.children;
			d.children = null;
		} else {
			d.children = d._children;
			d._children = null;
		}
		update(d);
	}

	function click(d) {
		filterByClickedCategory(d.name, d.depth);
		d3.event.preventDefault();
        return false;
	}
}

function filterByClickedCategory(name, depth){
	filterByCategoryCommand([{name:'catname', value:name},{name:'catlevel', value:depth}]);
}



