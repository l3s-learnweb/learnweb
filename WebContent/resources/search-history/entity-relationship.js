
//draw query path
var tip = d3.tip()
			.attr('class', 'd3-tip')
			.offset([-10, 0])
			.html(function(d) {
				var string1 = "<ul>";
			
				for(var i = 0; i < d.data.entities.length; i++){
					//string1 += "<li>" + d.data.entities[i] + "</li>";
					string1 += "<li>" + d.data.entities[i].entity_name + "</li>";
				}
				string1 += "</ul>";
				return string1;
		});

function drawQueryPath()
{
	var i;
	var G = new jsnx.DiGraph();
	/*var queryStrs = document.getElementById("queries").innerHTML.split(",");
    var queArray = [];
    for (i = 0; i < queryStrs.length-1; i++)
    {
    	queArray.push(queryStrs[i].trim());
    }
    var len = queArray.length;
    console.log(queArray);
    console.log(len);
	
    var relatedStrs = document.getElementById("related").innerHTML.split(";");
    var related = [];
    for (i = 0; i < relatedStrs.length-1; i++)
    {
    	var relatedStr = relatedStrs[i].trim().split(",");
    	var relatedArr = [];
    	for (j = 0; j < relatedStr.length-1; j++)
    	{
    		relatedArr.push(relatedStr[j].trim());
    	}
    	related.push(relatedArr);
    }
    var len_3 = related.length;
    console.log(related);*/
	
	//add query nodes		
	/*for(i=0; i &lt; len; i++){
		//var nodeA = {toString: function(){return queArray[i]}}
		//console.log(enArray[i]);
		//console.log(JSON.parse(enArray[i].entities));
		G.addNode(queArray[i], {count:20, color: '#489a83', entities: related[i]});
	}*/
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