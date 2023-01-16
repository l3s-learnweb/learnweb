const url = '../resources/dataExample.json';

const onLoad = (graph) => {
  // eslint-disable-next-line no-undef
  d3.select('svg').selectAll('*').remove();
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
    // eslint-disable-next-line no-undef,no-use-before-define
    .force('link', d3.forceLink().distance(300))
    // eslint-disable-next-line no-undef
    .force('charge', d3.forceManyBody().strength(-700))
    // eslint-disable-next-line no-undef
    .force('center', d3.forceCenter(1000 / 2, 1000 / 2))
    // eslint-disable-next-line no-undef,no-use-before-define
    .force('collide', d3.forceCollide().radius((d) => fibonacci(d.frequency + 4) * 16));

  init();

  function init() {
    links = linkContainer.selectAll('.link')
      .data(graph.record.links)
      .join('line')
      .attr('class', 'link')
      .attr('marker-end', 'url(#arrowhead)')
      .style('stroke', 'black')
      .text((d) => d.user);

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

    // eslint-disable-next-line no-undef
    nodes.selectAll('circle')
      .data((d) => [d])
      .join('circle')
      // eslint-disable-next-line no-use-before-define
      .attr('r', (d) => fibonacci(d.frequency + 4) * 7)
      .style('fill', (d) => {
        console.log('d: ', d);
        return color(d.user);
      });

    // eslint-disable-next-line no-undef
    nodes.selectAll('text')
      .data((d) => [d])
      .join('text')
      .attr('dominant-baseline', 'central')
      .attr('text-anchor', 'middle')
      .attr('id', (d) => `text${d.id}`)
      .attr('pointer-events', 'none')
      .style('font-size', '14px')
      .text((d) => d.name);

    nodes.append('text')
      // eslint-disable-next-line no-use-before-define
      .attr('dx', (d) => fibonacci(d.frequency + 4) * 7 * Math.sin(45) + 3)
      // eslint-disable-next-line no-use-before-define
      .attr('dy', (d) => fibonacci(d.frequency + 4) * 7 * Math.sin(45) + 3)
      .attr('cx', 250)
      .attr('cy', 100)
      .attr('pointer-events', 'none')
      .text((d) => d.user)
      .style('fill', (d) => colorLabel(d.user))
      // eslint-disable-next-line no-use-before-define
      .style('font-size', '12px');

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
  if (!url) return;
  this.url = url;
  const root = JSON.parse(url);
  // eslint-disable-next-line no-undef
  onLoad(root);
}

const fibonacci = (n) => {
  let a = 0; let b = 1; let
    c = n;

  for (let i = 2; i <= n; i++) {
    c = a + b;
    a = b;
    b = c;
  }

  return c;
};
