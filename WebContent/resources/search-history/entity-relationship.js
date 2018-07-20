//function to execute on page load
$(document).ready(function(){
	$('.hideIcon').on('click',function(){
		$('.snippet-viewer').hide();
	});

	$('.session_block').on('click', function(e){
		var element = e.currentTarget;
		var sessionId = element.getAttribute("data-sessionid"); 
		$('.box ul li').css("background","#489a83");
		$(element).parent().css("background","darkgrey");
		updateKG([
		    {name: "session-id", value: sessionId }
		]);
	});

	$('#query_path_button').hide();
});

//function to highlight selected snippets for particular entity
function filterSnippets()
{
	//The below code removes all snippets that doesn't contain the selected entity
	/*$('.snippet').each(function(){
				search_id = $(this).data('searchid');
				rank = $(this).data('rank');
				if(!searchRanks.get(search_id).includes(rank))
					$(this).hide();
			});*/

	//Highlights the snippets that contains clicked entity
	searchRanks.forEach(function(value, key){
		value.reverse();
		for(var i = 0; i < value.length; i++)
		{	
			$('.snippet[data-searchid="' + key + '"][data-rank="'+ value[i] + '"]').addClass('selected_snippet');//.css('background-color', 'rgba(72, 154, 131, 0.22)');
			var detachedSnippet = $('.snippet[data-searchid="' + key + '"][data-rank="'+ value[i] + '"]').detach();
			$('#snippets div:first-child').after(detachedSnippet);
		}
	});

	$('.snippet-viewer').show();
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
	for(i = 0; i < queriesJsonArr.length; i++){
		if(i == 0)
			G.addNode(queriesJsonArr[i].query, {count: width/75, color: 'yellowgreen', type: 'query', search_id: queriesJsonArr[i].search_id});
		else
			G.addNode(queriesJsonArr[i].query, {count: width/75, color: '#489a83', type: 'query', search_id: queriesJsonArr[i].search_id});
	}
	
	//add edge relationshipss
	for(i = 0; i < edgesJsonArr.length; i++){
		var edge = edgesJsonArr[i];
		G.addEdge(edge.source, edge.target, {color: '#d2dde0', score: edge.score});
	}
	console.log(edgesJsonArr.length);
	
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
				return d.data.score == undefined ? 'related-edge' : 'score-edge';
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
				return d.data.score == undefined ? d.data.color : d3.interpolateGreens(colorScale(d.data.score));
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
	    if(d.data.type == 'query')
	    {
	    	setSelectedSearchId([
	    	 	{name: "search-id", value: d.data.search_id },
	    	 	{name: "entity-name", value: d.node}
	    	]);
	    }else if(d.data.type == 'entity'){
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
}

//change opacity of those aren't in the map
var toggle = 0;
function changeEdgeOpacity(e, selectedNode){
	return selectedNode == e.source.node || selectedNode == e.target.node ? 1 : 0.1;
}

function connectedNodes(d){
	var node = d3.selectAll('.node');
	//var edge = d3.selectAll('.edge');
	var scoreEdge = d3.selectAll('#score-edge');
	var relatedEdge = d3.selectAll('#related-edge');
	if(toggle == 0){
		var sj = d.node;
		var nodes = d.G.neighbors(d.node).concat(d.node);
		node.style("opacity", function(o){
			var ob = o.node;
			return nodes.includes(ob) ? 1:0.1;
		});
		/*edge.style("opacity", function(e){
			return sj == e.source.node| sj ==e.target.node ? 1:0.1;
		});*/
		scoreEdge.style("opacity", function(e){return changeEdgeOpacity(e, sj);});
		relatedEdge.style("opacity",function(e){return changeEdgeOpacity(e, sj);});
		toggle = 1;
	}
}

//reset when mouse out
function recoverNodes(){
	d3.selectAll('.node').style("opacity", 1);
	//d3.selectAll('.edge').style("opacity", 1);
	d3.selectAll('#score-edge').style("opacity", 0.5);
	d3.selectAll('#related-edge').style("opacity", 1);
	toggle = 0;
}

function drawQueryPath()
{
	var i;
	var G = new jsnx.DiGraph();
	
	//add query nodes		
	for(i = 0; i < queriesJsonArr.length; i++){
		G.addNode(queriesJsonArr[i].query, {count:20, color: '#489a83', entities: queriesJsonArr[i].related_entities});
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