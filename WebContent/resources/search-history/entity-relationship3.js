/* global queriesJsonArr, edgesJsonArr, selectedGroupId */
/* global updateKG, setSelectedSearchId, setSelectedSearchIds, setSelectedGroupId */

// function to execute on page load
$(document).on('click', '.session_block', function(e){
    hideSnippet();

    var element = e.currentTarget;
    var sessionId = element.getAttribute("data-sessionid");
    var userId = element.getAttribute("data-userid");

	$('.box ul li').css("background", "");

    $(element).parent().css("background", "#8da73d");
    updateKG([
        {name: "session-id", value: sessionId},
        {name: "user-id", value: userId}
    ]);
});

$(document).on('click', '#filterByCategories', function(){
    window.categories = [];
    var selectedCategories = document.querySelectorAll('[name="categoriesList"]:checked');
    selectedCategories.forEach(function (c) {
        window.categories.push(c.value);
    });
    recoverNodes();
});

$(window).on('resize', function(){
    resizeGraph();
});

function showSnippet()
{
	var $graph = $('#knowledge-graph');
	var $snippet = $('#knowledge-snippet');

	$graph.removeClass('ui-g-12 ui-md-12 ui-lg-12').addClass('ui-g-9 ui-md-9 ui-lg-9');
    $snippet.show();
    resizeGraph();
}

function hideSnippet()
{
    var $graph = $('#knowledge-graph');
    var $snippet = $('#knowledge-snippet');

    $snippet.hide();
    $graph.removeClass('ui-g-9 ui-md-9 ui-lg-9').addClass('ui-g-12 ui-md-12 ui-lg-12');
    resizeGraph();
}

function selectGroupId()
{
	setSelectedGroupId([
		// the group id is temporary hard coded.
	    {name: "group-id", value: 419 }
	]);
}

//function to highlight selected snippets for particular entity
function filterSnippets()
{
	//The below code removes all snippets that doesn't contain the selected entity
	$('.snippet').each(function(){
				search_id = $(this).data('searchid');
				rank = $(this).data('rank');
				if(!searchRanks.get(search_id).includes(rank))
					$(this).hide();
			});

	//Highlights the snippets that contains clicked entity
	/*searchRanks.forEach(function(value, key){
		value.reverse();
		for(var i = 0; i < value.length; i++)
		{
			$('.snippet[data-searchid="' + key + '"][data-rank="'+ value[i] + '"]').addClass('selected_snippet');//.css('background-color', 'rgba(72, 154, 131, 0.22)');
			var detachedSnippet = $('.snippet[data-searchid="' + key + '"][data-rank="'+ value[i] + '"]').detach();
			$('#snippets div:first-child').after(detachedSnippet);
		}
	});*/

    showSnippet();
}

//draw query path
var tip = d3.tip()
			.attr('class', 'd3-tip')
			.offset([-10, 0])
			.html(function(d) {
				var string1 = "<ul>";

				for(var i = 0; i < d.data.entities.length; i++){
					string1 += "<li>" + d.data.entities[i].entity_name + "</li>";
				}
				string1 += "</ul>";
				return string1;
		});

var searchRanks;
var colorScale = d3.scaleLinear().domain([0, 1]).range([0.1, 0.5]);
//var linked = new Map();
function draw()
{
	var width = $('#canvas').width();
	var height = $('#canvas').height();
	console.log("height: " + height + " width: " + width);
	var i, j;
	var G = new jsnx.Graph();

    var entities = new Set();
    var entityRanksMap = new Map();
    for (i = 0; i < queriesJsonArr.length; i++)
    {
    	var queryObj = queriesJsonArr[i];
    	var related_entities = queryObj.related_entities;
    	for(j = 0; j < related_entities.length; j++)
    	{
    		var entity = related_entities[j];
    		entities.add(queriesJsonArr[i].query);
    		entities.add(entity.entity_name);
    		if(entityRanksMap.has(entity.entity_name))
    			entityRanksMap.get(entity.entity_name).set(queryObj.search_id, entity.ranks);//.push({search_id: queryObj.search_id, ranks: entity.ranks});
    		else
    			entityRanksMap.set(entity.entity_name, new Map([[queryObj.search_id, entity.ranks]]));//[{search_id: queryObj.search_id, ranks: entity.ranks}]);
    	}
    }

	//add node entities
	entities.forEach(function(entity){
		G.addNode(entity, {count: width/150, color: '#84cccc', type: 'entity', entity_name: entity});
	});

	//queries
	for(i = 0; i < queriesJsonArr.length; i++) {
		var singleQuery = queriesJsonArr[i];
		G.addNode(singleQuery.query, {
			count: width/75,
			color: i === 0 ? 'yellowgreen' : (selectedGroupId === 0 ? "#489a83" : "#919191"),
			type: 'query',
			search_id: singleQuery.search_id,
			categories: singleQuery.categories
		});
	}

	//add edge relationshipss
	for(i = 0; i < edgesJsonArr.length; i++){
		var edge = edgesJsonArr[i];
		G.addEdge(edge.source, edge.target, {color: '#d2dde0', score: edge.score});
	}

	//edges between queries and entities
	for(i = 0; i < queriesJsonArr.length; i++){
		var related_entities = queriesJsonArr[i].related_entities;
		var query = queriesJsonArr[i].query;
		for(j = 0; j < related_entities.length; j++){
			G.addEdge(query, related_entities[j].entity_name, {color:'#99c2ff'});
		}
	}

	//step edges between query nodes
	for(i=0; i < queriesJsonArr.length - 1; i++){
		G.addEdge(queriesJsonArr[i].query, queriesJsonArr[i + 1].query, {color: "black"});
	}

	jsnx.draw(G,{
		element: "#canvas",
		layoutAttr:{
			charge:-300,
			linkDistance: width/6
		},
		withLabels: true,
		nodeAttr:{
			r: function(d){
				if(width>1200){
					return d.data.count;
				}else{
					return d.data.count*1.5;
				}
			}
		},
		edgeAttr:{
			id: function(d){
				return d.data.score ? 'score-edge' : 'related-edge';
			}
		},
		nodeStyle:{
			fill:function(d){
				return d.data.color;
			},
			stroke: 'none'
		},
		labelStyle:{
			fill: 'black',
			'font-size': 10
		},
		edgeStyle: {
			fill:function(d){
				//return d.data.color;
				//console.log(d.edge + ": " + d.data.score + ": " + d3.interpolateGreys(d.data.score));
				//return d.data.score == undefined ? d.data.color : d3.interpolateGreys(colorScale(d.data.score));
				return d.data.score ? d3.interpolateGreens(colorScale(d.data.score)) : d.data.color;
			}
			/*stroke: function(d){
				console.log(d.edge + ": " + d.data.score);
				return d.data.score ? d.data.color : d3.interpolateGreens(d.data.score);
			}*/
		},
		stickyDrag: true
	});

	//Adding opacity of 0.5 to score edges which have green color scale
	d3.selectAll('#score-edge').style("opacity", 0.5);

	d3.selectAll('.node').on('dblclick', function(d) {
	    if(d.data.type === 'query') {
	    	setSelectedSearchId([
	    	 	{name: "search-id", value: d.data.search_id },
	    	 	{name: "entity-name", value: d.node}
	    	]);
	    } else if(d.data.type === 'entity') {
	    	searchRanks = entityRanksMap.get(d.data.entity_name);
	    	var search_ids = "";
	    	searchRanks.forEach(function(value, key){
	    		search_ids += key + ",";
	    	});

	    	setSelectedSearchIds([
	    		{name: "search-ids", value: search_ids },
	    		{name: "entity-name", value: d.data.entity_name}
	    	]);
	    }
	})
	.on('mouseover', connectedNodes)
	.on('mouseout', recoverNodes);

    $('#tagging-panel').show();
}

function resizeGraph() {
    var canvas = $('#canvas');
    $('svg.jsnx').attr("width", canvas.width()).attr("height", canvas.height());
}

//change opacity of those aren't in the map
var toggle = 0;

function connectedNodes(d){
	if(toggle === 0) {
        var nodes = d3.selectAll('.node');
        var edges = d3.selectAll('.edge');

		var selectedNode = d.node;
		var neighbors = d.G.neighbors(selectedNode).concat(selectedNode);
		nodes.style("opacity", function(o){return neighbors.includes(o.node) ? 1 : 0.1;});
        edges.style("opacity", function(o){return selectedNode === o.source.node || selectedNode === o.target.node ? 1 : 0.1;});
		toggle = 1;
	}
}

//reset when mouse out
function recoverNodes(){
    if (window.categories === undefined || window.categories.length === 0) {
        d3.selectAll('.node').style("opacity", 1);
        // d3.selectAll('.edge').style("opacity", 1);
        // d3.selectAll('.edge #score-edge').style("opacity", 0.5);
        // d3.selectAll('.edge #related-edge').style("opacity", 1);
        d3.selectAll('.edge').style("opacity", function (e) {
            return this.childNodes[0].id === 'related-edge' ? 1 : 0.5;
        });
        toggle = 0;
        return;
    }

    var nodes = d3.selectAll('.node');
    var edges = d3.selectAll('.edge');

    var activeNodes = [];
    nodes.style("opacity", function(e){
        var isIncludes = Object.keys(e.data.categories).some(function (cat) { return window.categories.includes(cat) });
        if (isIncludes) activeNodes.push(e.node);
        return isIncludes ? 1 : 0.25;
    });

    edges.style("opacity", function(e){
        return activeNodes.includes(e.source.node) && activeNodes.includes(e.target.node) ? 1 : 0.25;
    });

	toggle = 0;
}

function drawQueryPath()
{
	var i;
	var G = new jsnx.DiGraph();

	//add query nodes
	for(i = 0; i < queriesJsonArr.length; i++){
		G.addNode(queriesJsonArr[i].query, {count: 20, color: '#489a83', entities: queriesJsonArr[i].related_entities});
	}

	//add query edges
	for(i = 0; i < queriesJsonArr.length - 1; i++){
		G.addEdge(queriesJsonArr[i].query, queriesJsonArr[i + 1].query);
	}

	jsnx.draw(G,{
		element: "#canvas",
		layoutAttr:{
			charge:-200,
			linkDistance:150
		},
		withLabels: true,
		nodeAttr:{
			r: function(d){
				return d.data.count;
			}
		},
		nodeStyle:{
			fill:function(d){
				return d.data.color;
			},
			stroke: 'none'
		},
		labelStyle:{
			fill: 'black'
		},
		stickyDrag: true
	});

	d3.select('svg').call(tip);
	d3.selectAll('.node').on('dblclick',tip.show);
	d3.selectAll('.node').on('mouseout', tip.hide);
}