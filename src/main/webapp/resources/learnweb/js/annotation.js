const url = '../resources/dataExample.json';

const onLoad = (graph) => {
  console.log('G: ', graph);
  // eslint-disable-next-line no-undef
  const svg = d3.select('svg')
    // eslint-disable-next-line no-undef
    .call(d3.zoom().on('zoom', (event) => {
      svg.attr('transform', event.transform);
    }))
    .append('g');
  // eslint-disable-next-line no-undef
  d3.select('svg').on('dblclick.zoom', null);
  // eslint-disable-next-line no-undef
  const color = d3.scaleOrdinal(d3.schemePastel1);
  // eslint-disable-next-line no-undef
  const colorLabel = d3.scaleOrdinal(d3.schemeSet1);
  const linkContainer = svg.append('g').attr('class', 'linkContainer');
  const nodeContainer = svg.append('g').attr('class', 'nodeContainer');

  // eslint-disable-next-line no-undef
  const forceLayout = d3.forceSimulation()
    // eslint-disable-next-line no-undef
    .force('link', d3.forceLink().distance(125))
    // eslint-disable-next-line no-undef
    .force('charge', d3.forceManyBody().strength(-1250))
    // eslint-disable-next-line no-undef
    .force('center', d3.forceCenter(window.innerWidth / 2, window.innerHeight / 2))
    // eslint-disable-next-line no-undef
    .force('collision', d3.forceCollide().radius(50));

  init();

  function init() {
    links = linkContainer.selectAll('.link')
      .data(graph.record.links)
      .join('line')
      .attr('class', 'link')
      .attr('marker-end', 'url(#arrowhead)')
      .style('stroke', 'black')
      .text((d) => d.users);

    nodes = nodeContainer.selectAll('.node')
      .data(graph.record.nodes, (d) => d.id)
      .join('g')
      .attr('class', 'node')
      .attr('id', (d) => `node${d.id}`)
      // eslint-disable-next-line no-undef
      .call(d3.drag()
        .on('start', dragStarted)
        .on('drag', dragged)
        .on('end', dragEnded));

    nodes.selectAll('circle')
      .data((d) => [d])
      .join('circle')
      .attr('r', (d) => d.frequency * 9)
      .style('fill', (d) => {
        console.log('d: ', d);
        return color(d.group);
      });

    nodes.selectAll('text')
      .data((d) => [d])
      .join('text')
      .attr('dominant-baseline', 'central')
      .attr('text-anchor', 'middle')
      .attr('id', (d) => `text${d.id}`)
      .attr('pointer-events', 'none')
      .text((d) => d.query);

    nodes.append('text')
      .attr('dx', 50)
      .attr('dy', 50)
      .attr('cx', 250)
      .attr('cy', 100)
      .text((d) => d.users)
      .style('fill', (d) => colorLabel(d.users));

    forceLayout
      .nodes(graph.record.nodes)
      .on('tick', outerTick);

    forceLayout
      .force('link').links(graph.record.links);
  }

  function outerTick() {
    // eslint-disable-next-line no-undef
    links
      .attr('x1', (d) => d.source.x)
      .attr('y1', (d) => d.source.y)
      .attr('x2', (d) => d.target.x)
      .attr('y2', (d) => d.target.y);

    // eslint-disable-next-line no-undef
    nodes.attr('transform', (d) => `translate(${d.x},${d.y})`);
  }

  function dragStarted(event, d) {
    if (!event.active) forceLayout.alphaTarget(0.3).restart();
    d.fx = d.x;
    d.fy = d.y;
  }

  function dragged(event, d) {
    d.fx = event.x;
    d.fy = event.y;
  }

  function dragEnded(event, d) {
    if (!event.active) forceLayout.alphaTarget(0);
    d.fx = undefined;
    d.fy = undefined;
  }
};

// eslint-disable-next-line no-shadow
function SetUrl(url) {
  if (url != null) {
    this.url = url;
  }
  const root = JSON.parse(url);
  // eslint-disable-next-line no-undef
  onLoad(root);
}
