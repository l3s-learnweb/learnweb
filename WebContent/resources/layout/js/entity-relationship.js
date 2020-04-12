/* global queriesJsonArr, edgesJsonArr, selectedGroupId */
/* global updateKG, setSelectedSearchId, setSelectedSearchIds, setSelectedGroupId */
/* global d3, jsnx */

$(document).on('click', '.session_block', (e) => {
  hideKGSnippet();

  const element = e.currentTarget;
  const sessionId = element.getAttribute('data-sessionid');
  const userId = element.getAttribute('data-userid');

  updateKG([
    { name: 'session-id', value: sessionId },
    { name: 'user-id', value: userId },
  ]);
});

function selectGroupId() {
  setSelectedGroupId([
    // the group id is temporary hard coded.
    { name: 'group-id', value: 419 },
  ]);
}

$(document).on('click', '.sessions-list a', (e) => {
  $('.sessions-list li.active').removeClass('active');
  $(e.currentTarget).parent().addClass('active');
});

$(document).on('click', '.queries-list a', (e) => {
  $('.queries-list li.active').removeClass('active');
  $(e.currentTarget).parent().addClass('active');
});

$(window).on('resize', () => {
  resizeGraph();
});

function showKGSnippet() {
  const $queries = $('#queries');
  const $snippet = $('#snippetsList');

  $queries.removeClass('col-12 col-md-12 col-lg-12').addClass('col-6 col-md-6 col-lg-6');
  $snippet.show();
  resizeGraph();
}

function hideKGSnippet() {
  const $queries = $('#queries');
  const $snippet = $('#snippetsList');

  $snippet.hide();
  $queries.removeClass('col-6 col-md-6 col-lg-6').addClass('col-12 col-md-12 col-lg-12');
  resizeGraph();
}

// draw query path
const tip = d3.tip()
  .attr('class', 'd3-tip')
  .offset([-10, 0])
  .html((d) => {
    let string1 = '<ul>';

    for (let i = 0; i < d.data.entities.length; i++) {
      string1 += `<li>${d.data.entities[i].entity_name}</li>`;
    }
    string1 += '</ul>';
    return string1;
  });

let searchRanks;
const colorScale = d3.scaleLinear().domain([0, 1]).range([0.1, 0.5]);

// function to highlight selected snippets for particular entity
function filterSnippets() {
  // The below code removes all snippets that doesn't contain the selected entity
  $('.snippet').each((i, el) => {
    const searchId = $(el).data('searchid');
    const rank = $(el).data('rank');
    if (!searchRanks.get(searchId).includes(rank)) $(el).hide();
  });

  // Highlights the snippets that contains clicked entity
  /* searchRanks.forEach(function(value, key){
      value.reverse();
      for(var i = 0; i < value.length; i++)
      {
          //.css('background-color', 'rgba(72, 154, 131, 0.22)');
          $('.snippet[data-searchid="' + key + '"][data-rank="'+ value[i] + '"]').addClass('selected_snippet');
          var detachedSnippet = $('.snippet[data-searchid="' + key + '"][data-rank="'+ value[i] + '"]').detach();
          $('#snippets div:first-child').after(detachedSnippet);
      }
  }); */

  showKGSnippet();
}

// var linked = new Map();
function draw() {
  if (typeof queriesJsonArr === 'undefined' || !$('#knowledge-graph').length) return;

  const $canvas = $('#canvas');
  const width = $canvas.width();
  const height = $canvas.height();
  console.log(`height: ${height} width: ${width}`);
  let i;
  let j;
  const G = new jsnx.Graph();

  const entities = new Set();
  const entityRanksMap = new Map();
  for (i = 0; i < queriesJsonArr.length; i++) {
    const queryObj = queriesJsonArr[i];
    const { relatedEntities } = queryObj;
    for (j = 0; j < relatedEntities.length; j++) {
      const entity = relatedEntities[j];
      entities.add(queriesJsonArr[i].query);
      entities.add(entity.entity_name);
      if (entityRanksMap.has(entity.entity_name)) entityRanksMap.get(entity.entity_name).set(queryObj.search_id, entity.ranks);
      else entityRanksMap.set(entity.entity_name, new Map([[queryObj.search_id, entity.ranks]]));
    }
  }

  // add node entities
  entities.forEach((entity) => {
    G.addNode(entity, {
      count: width / 150, color: '#84cccc', type: 'entity', entity_name: entity,
    });
  });

  // queries
  for (i = 0; i < queriesJsonArr.length; i++) {
    const singleQuery = queriesJsonArr[i];
    const color = selectedGroupId === 0 ? '#4aa382' : '#919191';
    G.addNode(singleQuery.query, {
      count: width / 75,
      color: i === 0 ? 'yellowgreen' : color,
      type: 'query',
      search_id: singleQuery.search_id,
      categories: singleQuery.categories,
    });
  }

  // add edge relationships
  for (i = 0; i < edgesJsonArr.length; i++) {
    const edge = edgesJsonArr[i];
    G.addEdge(edge.source, edge.target, { color: '#d2dde0', score: edge.score });
  }

  // edges between queries and entities
  for (i = 0; i < queriesJsonArr.length; i++) {
    const { relatedEntities } = queriesJsonArr[i];
    const { query } = queriesJsonArr[i];
    for (j = 0; j < relatedEntities.length; j++) {
      G.addEdge(query, relatedEntities[j].entity_name, { color: '#99c2ff' });
    }
  }

  // step edges between query nodes
  for (i = 0; i < queriesJsonArr.length - 1; i++) {
    G.addEdge(queriesJsonArr[i].query, queriesJsonArr[i + 1].query, { color: 'black' });
  }

  jsnx.draw(G, {
    element: '#canvas',
    layoutAttr: {
      charge: -300,
      linkDistance: width / 6,
    },
    withLabels: true,
    nodeAttr: {
      r(d) {
        if (width > 1200) {
          return d.data.count;
        }
        return d.data.count * 1.5;
      },
    },
    edgeAttr: {
      id(d) {
        return d.data.score ? 'score-edge' : 'related-edge';
      },
    },
    nodeStyle: {
      fill(d) {
        return d.data.color;
      },
      stroke: 'none',
    },
    labelStyle: {
      fill: 'black',
      'font-size': 10,
    },
    edgeStyle: {
      fill(d) {
        // return d.data.color;
        // console.log(d.edge + ": " + d.data.score + ": " + d3.interpolateGreys(d.data.score));
        // return d.data.score == undefined ? d.data.color : d3.interpolateGreys(colorScale(d.data.score));
        return d.data.score ? d3.interpolateGreens(colorScale(d.data.score)) : d.data.color;
      },
      /* stroke: function(d){
          console.log(d.edge + ": " + d.data.score);
          return d.data.score ? d.data.color : d3.interpolateGreens(d.data.score);
      } */
    },
    stickyDrag: true,
  });

  // Adding opacity of 0.5 to score edges which have green color scale
  d3.selectAll('#score-edge').style('opacity', 0.5);

  d3.selectAll('.node').on('dblclick', (d) => {
    if (d.data.type === 'query') {
      setSelectedSearchId([
        { name: 'search-id', value: d.data.search_id },
        { name: 'entity-name', value: d.node },
      ]);
    } else if (d.data.type === 'entity') {
      searchRanks = entityRanksMap.get(d.data.entity_name);
      let searchIds = '';
      searchRanks.forEach((value, key) => {
        searchIds += `${key},`;
      });

      setSelectedSearchIds([
        { name: 'search-ids', value: searchIds },
        { name: 'entity-name', value: d.data.entity_name },
      ]);
    }
  })
    .on('mouseover', connectedNodes)
    .on('mouseout', recoverNodes);
}

function resizeGraph() {
  const canvas = $('#canvas');
  $('svg.jsnx').attr('width', canvas.width()).attr('height', canvas.height());
}

// change opacity of those aren't in the map
let toggle = 0;

function connectedNodes(d) {
  if (toggle === 0) {
    const nodes = d3.selectAll('.node');
    const edges = d3.selectAll('.edge');

    const selectedNode = d.node;
    const neighbors = d.G.neighbors(selectedNode).concat(selectedNode);
    nodes.style('opacity', (o) => (neighbors.includes(o.node) ? 1 : 0.1));
    edges.style('opacity', (o) => (selectedNode === o.source.node || selectedNode === o.target.node ? 1 : 0.1));
    toggle = 1;
  }
}

// reset when mouse out
function recoverNodes() {
  if (window.categories === undefined || window.categories.length === 0) {
    d3.selectAll('.node').style('opacity', 1);
    // d3.selectAll('.edge').style("opacity", 1);
    // d3.selectAll('.edge #score-edge').style("opacity", 0.5);
    // d3.selectAll('.edge #related-edge').style("opacity", 1);
    d3.selectAll('.edge').style('opacity', function () {
      return this.childNodes[0].id === 'related-edge' ? 1 : 0.5;
    });
    toggle = 0;
    return;
  }

  const nodes = d3.selectAll('.node');
  const edges = d3.selectAll('.edge');

  const activeNodes = [];
  const activeMinorNodes = [];
  nodes.style('opacity', (e) => {
    if (e.data.type === 'query') {
      const isIncludes = e.data.categories ? Object.keys(e.data.categories).some((cat) => window.categories.includes(cat)) : false;
      if (isIncludes) activeNodes.push(e.node);
      return isIncludes ? 1 : 0.25;
    }
    const neighbors = e.G.neighbors(e.node);
    let neighborsIncludes = false;
    if (neighbors.length) {
      neighborsIncludes = Array.from(neighbors).some((neighbor) => {
        const node = e.G.node.get(neighbor);
        return node.categories && Object.keys(node.categories).some((cat) => window.categories.includes(cat));
      });
    }

    if (neighborsIncludes) activeMinorNodes.push(e.node);
    return neighborsIncludes ? 1 : 0.25;
  });

  edges.style('opacity', (e) => {
    if (activeNodes.includes(e.source.node) && activeNodes.includes(e.target.node)) {
      return 1;
    }

    if (activeNodes.includes(e.source.node) && activeMinorNodes.includes(e.target.node)) {
      return 1;
    }

    return 0.25;
  });

  toggle = 0;
}

function drawQueryPath() {
  let i;
  const G = new jsnx.DiGraph();

  // add query nodes
  for (i = 0; i < queriesJsonArr.length; i++) {
    G.addNode(queriesJsonArr[i].query, { count: 20, color: '#4aa382', entities: queriesJsonArr[i].relatedEntities });
  }

  // add query edges
  for (i = 0; i < queriesJsonArr.length - 1; i++) {
    G.addEdge(queriesJsonArr[i].query, queriesJsonArr[i + 1].query);
  }

  jsnx.draw(G, {
    element: '#canvas',
    layoutAttr: {
      charge: -200,
      linkDistance: 150,
    },
    withLabels: true,
    nodeAttr: {
      r(d) {
        return d.data.count;
      },
    },
    nodeStyle: {
      fill(d) {
        return d.data.color;
      },
      stroke: 'none',
    },
    labelStyle: {
      fill: 'black',
    },
    stickyDrag: true,
  });

  d3.select('svg').call(tip);
  d3.selectAll('.node').on('dblclick', tip.show);
  d3.selectAll('.node').on('mouseout', tip.hide);
}