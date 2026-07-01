/* ============================================================
   ESTADO DE LA INTERFAZ
   Los datos importantes viven en el backend y en la base de datos.
   Este objeto solo conserva una copia temporal para pintar la pantalla.
   ============================================================ */
const state = {
    dashboard: null,
    products: [],
    clients: [],
    tree: null,
    orders: [],
    queue: [],
    graph: { nodos: [], aristas: [] },
    route: null,
    history: [],
    decisions: []
};

const viewTitles = {
    dashboard: 'Panel principal',
    productos: 'Catálogo de perfumes',
    clientes: 'Clientes',
    arbol: 'Árbol binario con IA',
    pedidos: 'Gestión de pedidos',
    fifo: 'Cola FIFO',
    rutas: 'Grafo y rutas con IA',
    historial: 'Historial LIFO',
    decisiones: 'Decisiones de IA'
};

// El zoom solo afecta la representación visual del árbol.
let treeZoom = 1;


/* ============================================================
   COMUNICACIÓN CON EL BACKEND
   Todas las peticiones pasan por esta función para que los errores
   se muestren de la misma manera en cualquier módulo.
   ============================================================ */
async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        ...options
    });

    const raw = await response.text();
    let data = null;
    if (raw) {
        try { data = JSON.parse(raw); } catch { data = raw; }
    }

    if (!response.ok) {
        const message = data?.message || `No se pudo completar la operación (${response.status}).`;
        throw new Error(message);
    }
    return data;
}

async function loadAll(showSuccess = false) {
    try {
        const [dashboard, products, clients, tree, orders, queue, graph, route, history, decisions] = await Promise.all([
            api('/api/dashboard'),
            api('/api/productos'),
            api('/api/clientes'),
            api('/api/clientes/arbol'),
            api('/api/pedidos'),
            api('/api/pedidos/cola'),
            api('/api/rutas/grafo'),
            api('/api/rutas/ultima'),
            api('/api/historial'),
            api('/api/decisiones-ia')
        ]);

        Object.assign(state, { dashboard, products, clients, tree, orders, queue, graph, route, history, decisions });
        renderAll();
        if (showSuccess) toast('Datos actualizados', 'La información se volvió a cargar desde la base de datos.');
    } catch (error) {
        toast('No se pudo cargar el sistema', error.message, true);
    }
}

/* ============================================================
   NAVEGACIÓN
   La página funciona como una SPA sencilla: no se recarga al cambiar
   de módulo, solo se oculta una sección y se muestra otra.
   ============================================================ */
function showView(name) {
    document.querySelectorAll('.view').forEach(view => view.classList.remove('active'));
    document.getElementById(`view-${name}`).classList.add('active');
    document.querySelectorAll('.nav-item').forEach(button => {
        button.classList.toggle('active', button.dataset.view === name);
    });
    document.getElementById('pageTitle').textContent = viewTitles[name];
    document.getElementById('sidebar').classList.remove('open');
    window.scrollTo({ top: 0, behavior: 'smooth' });

    // Al entrar al módulo del árbol, lo ajustamos al ancho disponible.
    if (name === 'arbol') {
        requestAnimationFrame(fitTreeToViewport);
    }
}

document.querySelectorAll('[data-view]').forEach(button => {
    button.addEventListener('click', () => showView(button.dataset.view));
});
document.querySelectorAll('[data-go]').forEach(button => {
    button.addEventListener('click', () => showView(button.dataset.go));
});
document.getElementById('mobileMenu').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
});
document.getElementById('refreshButton').addEventListener('click', () => loadAll(true));

document.getElementById('treeZoomIn').addEventListener('click', () => changeTreeZoom(0.1));
document.getElementById('treeZoomOut').addEventListener('click', () => changeTreeZoom(-0.1));
document.getElementById('treeFit').addEventListener('click', fitTreeToViewport);
document.getElementById('nodeModalClose').addEventListener('click', closeNodeModal);
document.querySelector('[data-close-node-modal]').addEventListener('click', closeNodeModal);
document.addEventListener('keydown', event => {
    if (event.key === 'Escape') closeNodeModal();
});
window.addEventListener('resize', () => {
    if (document.getElementById('view-arbol').classList.contains('active')) {
        fitTreeToViewport();
    }
});

/* ============================================================
   FORMULARIOS
   Cada formulario envía JSON al backend. Después se recargan los datos
   para que tablas, árbol, cola y panel queden sincronizados.
   ============================================================ */
document.getElementById('productForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    values.presentacionMl = Number(values.presentacionMl);
    values.precio = Number(values.precio);
    values.stock = Number(values.stock);

    await runAction(async () => {
        await api('/api/productos', { method: 'POST', body: JSON.stringify(values) });
        form.reset();
        form.elements.presentacionMl.value = 100;
        form.elements.stock.value = 10;
        await loadAll();
        toast('Perfume guardado', 'El catálogo y el stock fueron actualizados.');
    });
});

document.getElementById('clientForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));

    await runAction(async () => {
        const result = await api('/api/clientes', { method: 'POST', body: JSON.stringify(values) });
        form.reset();
        await loadAll();
        toast('Cliente ubicado', `${result.abreviatura} fue insertado en el árbol.`);
        showView('arbol');
    });
});

document.getElementById('orderForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    values.clienteId = Number(values.clienteId);
    values.productoId = Number(values.productoId);
    values.cantidad = Number(values.cantidad);

    await runAction(async () => {
        const result = await api('/api/pedidos', { method: 'POST', body: JSON.stringify(values) });
        form.reset();
        form.elements.cantidad.value = 1;
        await loadAll();
        toast('Pedido registrado', `${result.codigo} ingresó al final de la cola FIFO.`);
    });
});

document.getElementById('attendNextButton').addEventListener('click', () => runAction(async () => {
    const order = await api('/api/pedidos/atender-siguiente', { method: 'POST' });
    await loadAll();
    toast('Pedido preparado', `${order.codigo} ya está listo para ser incluido en una ruta.`);
}));

document.getElementById('generateRouteButton').addEventListener('click', () => runAction(async () => {
    const route = await api('/api/rutas/generar', { method: 'POST', body: JSON.stringify({ pedidoIds: [] }) });
    await loadAll();
    toast('Ruta generada', `${route.codigo}: ${route.distanciaTotalKm} km estimados.`);
}));

document.getElementById('popHistoryButton').addEventListener('click', () => runAction(async () => {
    const removed = await api('/api/historial/ultimo', { method: 'DELETE' });
    await loadAll();
    toast('Cima retirada', removed ? removed.descripcion : 'La pila ya estaba vacía.');
}));

async function changeOrderState(id, estado) {
    await runAction(async () => {
        await api(`/api/pedidos/${id}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({ estado })
        });
        await loadAll();
        toast('Estado actualizado', `El pedido ahora está ${labelEnum(estado).toLowerCase()}.`);
    });
}

async function runAction(action) {
    try {
        await action();
    } catch (error) {
        toast('Revisa la operación', error.message, true);
    }
}

/* ============================================================
   RENDER GENERAL
   Mantener las funciones separadas hace más sencillo explicar el código
   durante la exposición y modificar un módulo sin afectar a los demás.
   ============================================================ */
function renderAll() {
    renderDashboard();
    renderProducts();
    renderClients();
    renderTree();
    renderOrders();
    renderQueue();
    renderGraph();
    renderRouteResult();
    renderHistory();
    renderDecisions();
    fillSelects();
}

function renderDashboard() {
    const dashboard = state.dashboard || {};
    setText('kpiClients', dashboard.clientes ?? 0);
    setText('kpiProducts', dashboard.productos ?? 0);
    setText('kpiPending', dashboard.pedidosPendientes ?? 0);
    setText('kpiReady', dashboard.pedidosListos ?? 0);
    setText('lastDecision', dashboard.ultimaDecision || 'Aún no hay decisiones registradas.');

    const aiStatus = document.getElementById('aiStatus');
    aiStatus.classList.toggle('online', Boolean(dashboard.iaConfigurada));
    aiStatus.classList.toggle('offline', !dashboard.iaConfigurada);
    aiStatus.innerHTML = dashboard.iaConfigurada
        ? `<span class="status-dot"></span><div><strong>IA conectada</strong><small>${escapeHtml(dashboard.modeloIa || '')}</small></div>`
        : `<span class="status-dot"></span><div><strong>Modo local</strong><small>Falta GEMINI_API_KEY</small></div>`;

    const recent = state.orders.slice(0, 5);
    document.getElementById('dashboardOrders').innerHTML = recent.length
        ? recent.map(order => `<tr>
            <td><strong>${escapeHtml(order.codigo)}</strong></td>
            <td>${escapeHtml(order.clienteAbreviado)}</td>
            <td>${escapeHtml(order.producto)}</td>
            <td class="money">${money(order.total)}</td>
            <td>${status(order.estado)}</td>
        </tr>`).join('')
        : emptyRow(5, 'Todavía no hay pedidos registrados.');

    const route = dashboard.ultimaRuta || state.route;
    document.getElementById('lastRouteMini').innerHTML = route
        ? `<span>Última ruta</span><strong>${escapeHtml(route.codigo)} · ${route.distanciaTotalKm} km</strong><small>${escapeHtml(route.ordenZonas.join(' → '))}</small>`
        : `<span>Ruta activa</span><strong>Sin ruta generada</strong><small>Prepara pedidos desde la cola FIFO.</small>`;
}

function renderProducts() {
    const query = document.getElementById('productSearch').value.trim().toLowerCase();
    const rows = state.products.filter(product =>
        [product.codigo, product.nombre, product.marca, product.familiaOlfativa]
            .some(value => String(value).toLowerCase().includes(query))
    );

    document.getElementById('productsTable').innerHTML = rows.length
        ? rows.map(product => `<tr>
            <td><strong>${escapeHtml(product.codigo)}</strong></td>
            <td><strong>${escapeHtml(product.nombre)}</strong><br><small>${escapeHtml(product.marca)}</small></td>
            <td>${escapeHtml(product.familiaOlfativa)}</td>
            <td>${product.presentacionMl} ml</td>
            <td class="money">${money(product.precio)}</td>
            <td class="${product.stock <= 5 ? 'stock-low' : ''}">${product.stock}</td>
        </tr>`).join('')
        : emptyRow(6, 'No se encontraron perfumes.');
}

function renderClients() {
    const query = document.getElementById('clientSearch').value.trim().toLowerCase();
    const rows = state.clients.filter(client =>
        [client.codigo, client.nombreCompleto, client.abreviatura, client.zona]
            .some(value => String(value).toLowerCase().includes(query))
    );

    document.getElementById('clientsTable').innerHTML = rows.length
        ? rows.map(client => `<tr>
            <td><strong>${escapeHtml(client.codigo)}</strong></td>
            <td>${escapeHtml(client.nombreCompleto)}</td>
            <td><strong>${escapeHtml(client.abreviatura)}</strong></td>
            <td>${escapeHtml(client.zona)}</td>
            <td>${client.nivel}</td>
        </tr>`).join('')
        : emptyRow(5, 'Todavía no hay clientes registrados.');
}

function renderTree() {
    const canvas = document.getElementById('treeCanvas');

    if (!state.tree) {
        canvas.innerHTML = `<div class="empty-state">
            <svg class="icon empty-icon"><use href="#icon-tree"></use></svg>
            <strong>Árbol vacío</strong>
            <span>Registra un cliente para crear la raíz.</span>
        </div>`;
        return;
    }

    const layout = calculateTreeLayout(state.tree);
    const edgeMarkup = layout.edges.map(edge => {
        const parent = layout.byId.get(edge.parentId);
        const child = layout.byId.get(edge.childId);
        const startY = parent.y + layout.nodeHeight / 2;
        const endY = child.y - layout.nodeHeight / 2;
        const middleY = (startY + endY) / 2;
        const labelX = (parent.x + child.x) / 2;
        const labelY = middleY - 7;
        const label = edge.side === 'IZQUIERDA' ? 'MENOR' : 'MAYOR';

        return `<g>
            <path class="tree-edge" d="M ${parent.x} ${startY} C ${parent.x} ${middleY}, ${child.x} ${middleY}, ${child.x} ${endY}" />
            <text class="tree-edge-label" x="${labelX}" y="${labelY}" text-anchor="middle">${label}</text>
        </g>`;
    }).join('');

    const nodeMarkup = layout.nodes.map(item => treeNodeSvg(item, layout)).join('');

    canvas.innerHTML = `<svg
        id="treeSvg"
        class="tree-svg"
        viewBox="0 0 ${layout.width} ${layout.height}"
        width="${layout.width}"
        height="${layout.height}"
        data-base-width="${layout.width}"
        data-base-height="${layout.height}"
        role="img"
        aria-label="Árbol binario de clientes">
        ${edgeMarkup}
        ${nodeMarkup}
    </svg>`;

    canvas.querySelectorAll('[data-tree-node]').forEach(node => {
        const openDetails = () => openNodeModal(Number(node.dataset.treeNode));
        node.addEventListener('click', openDetails);
        node.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                openDetails();
            }
        });
    });

    treeZoom = 1;
    if (document.getElementById('view-arbol').classList.contains('active')) {
        requestAnimationFrame(fitTreeToViewport);
    }
}

/**
 * Calcula una posición real para cada nodo.
 * Cuando falta un hijo, se reserva su espacio para que izquierda y derecha
 * no se mezclen visualmente.
 */
function calculateTreeLayout(root) {
    const nodeWidth = 190;
    const nodeHeight = 108;
    const horizontalGap = 54;
    const verticalGap = 158;
    const marginX = 70;
    const marginY = 70;
    const widthCache = new Map();

    function measure(node) {
        if (!node) return 0;
        if (widthCache.has(node.id)) return widthCache.get(node.id);

        const hasChildren = Boolean(node.izquierda || node.derecha);
        if (!hasChildren) {
            widthCache.set(node.id, nodeWidth);
            return nodeWidth;
        }

        const leftWidth = node.izquierda ? measure(node.izquierda) : nodeWidth;
        const rightWidth = node.derecha ? measure(node.derecha) : nodeWidth;
        const width = Math.max(nodeWidth, leftWidth + horizontalGap + rightWidth);
        widthCache.set(node.id, width);
        return width;
    }

    const nodes = [];
    const edges = [];
    let maxDepth = 0;

    function place(node, left, depth, parentId = null, side = 'RAIZ') {
        if (!node) return;
        maxDepth = Math.max(maxDepth, depth);

        const hasChildren = Boolean(node.izquierda || node.derecha);
        const leftWidth = hasChildren
            ? (node.izquierda ? measure(node.izquierda) : nodeWidth)
            : 0;
        const rightWidth = hasChildren
            ? (node.derecha ? measure(node.derecha) : nodeWidth)
            : 0;
        const subtreeWidth = hasChildren
            ? leftWidth + horizontalGap + rightWidth
            : nodeWidth;

        const item = {
            node,
            x: marginX + left + subtreeWidth / 2,
            y: marginY + depth * verticalGap,
            depth,
            side
        };
        nodes.push(item);

        if (parentId !== null) {
            edges.push({ parentId, childId: node.id, side });
        }

        if (node.izquierda) {
            place(node.izquierda, left, depth + 1, node.id, 'IZQUIERDA');
        }
        if (node.derecha) {
            place(node.derecha, left + leftWidth + horizontalGap, depth + 1, node.id, 'DERECHA');
        }
    }

    const contentWidth = measure(root);
    place(root, 0, 0);

    const width = Math.max(920, contentWidth + marginX * 2);
    const height = Math.max(560, marginY * 2 + nodeHeight + maxDepth * verticalGap);
    const byId = new Map(nodes.map(item => [item.node.id, item]));

    return { nodes, edges, byId, width, height, nodeWidth, nodeHeight };
}

function treeNodeSvg(item, layout) {
    const { node, x, y, depth } = item;
    const halfWidth = layout.nodeWidth / 2;
    const halfHeight = layout.nodeHeight / 2;
    const isRoot = depth === 0;

    return `<g
        class="tree-card ${isRoot ? 'root' : ''}"
        data-tree-node="${node.id}"
        tabindex="0"
        role="button"
        aria-label="Ver detalles de ${escapeHtml(node.nombreCompleto)}">
        <rect class="node-shadow" x="${x - halfWidth}" y="${y - halfHeight + 6}" width="${layout.nodeWidth}" height="${layout.nodeHeight}" rx="22" />
        <rect class="node-surface" x="${x - halfWidth}" y="${y - halfHeight}" width="${layout.nodeWidth}" height="${layout.nodeHeight}" rx="22" />
        <circle class="tree-node-icon-bg" cx="${x}" cy="${y - 28}" r="15" />
        <circle class="tree-node-icon-line" cx="${x}" cy="${y - 32}" r="4" />
        <path class="tree-node-icon-line" d="M ${x - 7} ${y - 19} C ${x - 5} ${y - 26}, ${x + 5} ${y - 26}, ${x + 7} ${y - 19}" />
        <text class="tree-node-title" x="${x}" y="${y + 3}">${escapeHtml(node.abreviatura)}</text>
        <text class="tree-node-meta" x="${x}" y="${y + 22}">${escapeHtml(node.codigo)} · nivel ${node.nivel}</text>
        <text class="tree-node-action" x="${x}" y="${y + 42}">Ver detalles</text>
    </g>`;
}

function openNodeModal(id) {
    const client = state.clients.find(item => item.id === id);
    if (!client) return;

    setText('nodeModalTitle', client.nombreCompleto);
    setText('nodeModalAbbreviation', client.abreviatura);
    setText('nodeModalCode', client.codigo);
    document.getElementById('nodeModalContent').innerHTML = `
        ${detailItem('Nombre completo', client.nombreCompleto)}
        ${detailItem('Código único', client.codigo)}
        ${detailItem('Ubicación en el árbol', `${labelEnum(client.lado)} · nivel ${client.nivel}`)}
        ${detailItem('Zona de entrega', client.zona)}
        ${detailItem('Dirección', client.direccion || 'No registrada')}
        ${detailItem('Teléfono', client.telefono || 'No registrado')}
        ${detailItem('Correo', client.email || 'No registrado')}
        ${detailItem('Explicación de la IA', client.explicacionInsercion || 'Sin explicación registrada.')}
    `;

    const modal = document.getElementById('nodeModal');
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
    document.body.classList.add('modal-open');
    document.getElementById('nodeModalClose').focus();
}

function closeNodeModal() {
    const modal = document.getElementById('nodeModal');
    if (!modal.classList.contains('open')) return;
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('modal-open');
}

function changeTreeZoom(delta) {
    treeZoom = Math.min(1.6, Math.max(0.4, treeZoom + delta));
    applyTreeZoom();
}

function applyTreeZoom() {
    const svg = document.getElementById('treeSvg');
    if (!svg) return;
    const baseWidth = Number(svg.dataset.baseWidth);
    const baseHeight = Number(svg.dataset.baseHeight);
    svg.style.width = `${baseWidth * treeZoom}px`;
    svg.style.height = `${baseHeight * treeZoom}px`;
}

function fitTreeToViewport() {
    const svg = document.getElementById('treeSvg');
    const viewport = document.getElementById('treeViewport');
    if (!svg || !viewport || viewport.clientWidth === 0) return;

    const baseWidth = Number(svg.dataset.baseWidth);
    const availableWidth = Math.max(320, viewport.clientWidth - 36);
    treeZoom = Math.min(1, Math.max(0.4, availableWidth / baseWidth));
    applyTreeZoom();
    viewport.scrollLeft = 0;
    viewport.scrollTop = 0;
}

function renderOrders() {
    const query = document.getElementById('orderSearch').value.trim().toLowerCase();
    const rows = state.orders.filter(order =>
        [order.codigo, order.clienteNombre, order.clienteAbreviado, order.producto, order.zonaEntrega, order.estado]
            .some(value => String(value).toLowerCase().includes(query))
    );

    document.getElementById('ordersTable').innerHTML = rows.length
        ? rows.map(order => `<tr>
            <td><strong>${escapeHtml(order.codigo)}</strong><br><small>${dateTime(order.fechaRegistro)}</small></td>
            <td>${escapeHtml(order.clienteAbreviado)}<br><small>${escapeHtml(order.zonaEntrega)}</small></td>
            <td>${escapeHtml(order.producto)}<br><small>${order.cantidad} unidad(es)</small></td>
            <td class="money">${money(order.total)}</td>
            <td><span class="priority ${order.prioridad}">${labelEnum(order.prioridad)}</span></td>
            <td>${status(order.estado)}</td>
            <td>${orderAction(order)}</td>
        </tr>`).join('')
        : emptyRow(7, 'No hay pedidos que mostrar.');

    document.querySelectorAll('[data-deliver]').forEach(button => {
        button.addEventListener('click', () => changeOrderState(Number(button.dataset.deliver), 'ENTREGADO'));
    });
}

function orderAction(order) {
    if (order.estado === 'EN_RUTA') {
        return `<button class="button small ghost" data-deliver="${order.id}">Entregado</button>`;
    }
    if (order.estado === 'PENDIENTE') return '<small>Usar FIFO</small>';
    if (order.estado === 'LISTO_PARA_ENVIO') return '<small>Esperando ruta</small>';
    return '<small>—</small>';
}

function renderQueue() {
    setText('queueCount', state.queue.length);
    document.getElementById('attendNextButton').disabled = state.queue.length === 0;
    document.getElementById('queueList').innerHTML = state.queue.length
        ? state.queue.map((order, index) => `<div class="queue-item">
            <div class="queue-position">${index + 1}</div>
            <div><h4>${escapeHtml(order.codigo)} · ${escapeHtml(order.clienteAbreviado)}</h4><p>${escapeHtml(order.producto)} · ${dateTime(order.fechaRegistro)}</p></div>
            <span class="priority ${order.prioridad}">${labelEnum(order.prioridad)}</span>
        </div>`).join('')
        : `<div class="empty-state"><strong>Cola vacía</strong><span>No hay pedidos pendientes por preparar.</span></div>`;
}

/* ============================================================
   DIBUJO DEL GRAFO
   Las coordenadas son solo visuales. La distancia real se toma de la
   base de datos y se procesa en Java, no del tamaño de las líneas.
   ============================================================ */
const baseCoordinates = {
    'Tienda': [90, 285],
    'Centro de Lima': [260, 145],
    'Pueblo Libre': [420, 80],
    'San Miguel': [330, 360],
    'San Isidro': [525, 270],
    'Miraflores': [650, 120],
    'Surco': [735, 340],
    'Ate': [525, 485]
};

function renderGraph() {
    const svg = document.getElementById('routeGraph');
    const coordinates = buildCoordinates(state.graph.nodos);
    const activeEdges = new Set();
    const visitedNodes = new Set(state.route?.recorridoCompleto || []);

    if (state.route?.recorridoCompleto) {
        for (let i = 0; i < state.route.recorridoCompleto.length - 1; i++) {
            activeEdges.add(edgeKey(state.route.recorridoCompleto[i], state.route.recorridoCompleto[i + 1]));
        }
    }

    const edgeMarkup = state.graph.aristas.map(edge => {
        const [x1, y1] = coordinates[edge.origen];
        const [x2, y2] = coordinates[edge.destino];
        const active = activeEdges.has(edgeKey(edge.origen, edge.destino));
        const mx = (x1 + x2) / 2;
        const my = (y1 + y2) / 2;
        return `<g>
            <line class="graph-edge ${active ? 'active' : ''}" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" />
            <text class="graph-label" x="${mx}" y="${my - 7}" text-anchor="middle">${edge.distanciaKm} km</text>
        </g>`;
    }).join('');

    const nodeMarkup = state.graph.nodos.map(name => {
        const [x, y] = coordinates[name];
        const classes = ['graph-node'];
        if (name === 'Tienda') classes.push('store');
        if (visitedNodes.has(name)) classes.push('visited');
        const lines = splitNodeName(name);
        return `<g class="${classes.join(' ')}" transform="translate(${x},${y})">
            <circle r="37"></circle>
            <text y="${lines.length > 1 ? -3 : 4}">${lines.map((line, i) => `<tspan x="0" dy="${i === 0 ? 0 : 14}">${escapeHtml(line)}</tspan>`).join('')}</text>
        </g>`;
    }).join('');

    svg.innerHTML = edgeMarkup + nodeMarkup;
}

function buildCoordinates(nodes) {
    const result = {};
    const unknown = nodes.filter(node => !baseCoordinates[node]);
    nodes.forEach(node => { if (baseCoordinates[node]) result[node] = baseCoordinates[node]; });
    unknown.forEach((node, index) => {
        const angle = (Math.PI * 2 * index) / Math.max(unknown.length, 1);
        result[node] = [450 + Math.cos(angle) * 300, 280 + Math.sin(angle) * 210];
    });
    return result;
}

function renderRouteResult() {
    const container = document.getElementById('routeResult');
    const route = state.route;
    if (!route) {
        container.innerHTML = `<span class="section-kicker">Resultado</span><h3>Sin ruta generada</h3><p>Primero atiende pedidos de la cola FIFO. Luego la IA podrá organizar las zonas listas para entrega.</p>`;
        return;
    }

    const steps = route.ordenZonas.map((zone, index) => {
        const deliveries = route.entregasPorZona?.[zone] || [];
        const detail = zone === 'Tienda'
            ? (index === 0 ? 'Punto de partida' : 'Retorno al almacén')
            : (deliveries.length ? deliveries.join(', ') : 'Zona de paso');
        return `<div class="route-step"><div class="route-step-line"><span class="route-step-dot"></span></div><div><strong>${escapeHtml(zone)}</strong><small>${escapeHtml(detail)}</small></div></div>`;
    }).join('');

    container.innerHTML = `
        <div class="panel-head"><div><span class="section-kicker">Resultado</span><h3>${escapeHtml(route.codigo)}</h3></div><span class="origin-badge ${route.origenDecision}">${route.origenDecision}</span></div>
        <div class="route-steps">${steps}</div>
        <div class="route-stats">
            <div class="route-stat"><small>Distancia</small><strong>${route.distanciaTotalKm} km</strong></div>
            <div class="route-stat"><small>Tiempo</small><strong>${route.tiempoTotalMinutos} min</strong></div>
        </div>
        <p><strong>Criterio:</strong> ${escapeHtml(route.criterio)}</p>
        <p>${escapeHtml(route.explicacion)}</p>
    `;
}

function renderHistory() {
    document.getElementById('historyList').innerHTML = state.history.length
        ? state.history.map(item => `<div class="timeline-item">
            <div class="timeline-marker"><span class="timeline-dot"></span></div>
            <div class="timeline-content"><strong>${escapeHtml(item.tipo)}</strong><p>${escapeHtml(item.descripcion)}</p></div>
            <time class="timeline-time">${dateTime(item.fecha)}</time>
        </div>`).join('')
        : `<div class="empty-state"><strong>Pila vacía</strong><span>Las acciones nuevas aparecerán aquí.</span></div>`;
}

function renderDecisions() {
    document.getElementById('decisionsGrid').innerHTML = state.decisions.length
        ? state.decisions.map(item => `<article class="decision-card">
            <div class="decision-card-head">
                <div><span class="section-kicker">${labelEnum(item.tipo)}</span><h3>${escapeHtml(item.decisionFinal)}</h3></div>
                <span class="origin-badge ${item.origen}">${item.origen}</span>
            </div>
            <p>${escapeHtml(item.explicacion)}</p>
            <code>${escapeHtml(prettyJson(item.entradaJson))}</code>
            <small>${dateTime(item.fecha)}</small>
        </article>`).join('')
        : `<article class="panel empty-state"><strong>Sin decisiones</strong><span>Registra clientes o genera una ruta.</span></article>`;
}

/* Completa los selectores a partir de los datos que ya existen. */
function fillSelects() {
    const clientSelect = document.getElementById('orderClient');
    const currentClient = clientSelect.value;
    clientSelect.innerHTML = `<option value="">Seleccionar cliente</option>` + state.clients.map(client =>
        `<option value="${client.id}">${escapeHtml(client.codigo)} · ${escapeHtml(client.abreviatura)}</option>`
    ).join('');
    clientSelect.value = currentClient;

    const productSelect = document.getElementById('orderProduct');
    const currentProduct = productSelect.value;
    productSelect.innerHTML = `<option value="">Seleccionar perfume</option>` + state.products
        .filter(product => product.stock > 0)
        .map(product => `<option value="${product.id}">${escapeHtml(product.nombre)} · S/ ${Number(product.precio).toFixed(2)} · stock ${product.stock}</option>`)
        .join('');
    productSelect.value = currentProduct;

    const zoneSelect = document.getElementById('clientZone');
    const currentZone = zoneSelect.value;
    const zones = state.graph.nodos.filter(node => node !== 'Tienda');
    zoneSelect.innerHTML = `<option value="">Seleccionar zona</option>` + zones.map(zone =>
        `<option value="${escapeHtml(zone)}">${escapeHtml(zone)}</option>`
    ).join('');
    zoneSelect.value = currentZone;
}

/* ============================================================
   UTILIDADES DE PRESENTACIÓN
   Son funciones pequeñas para no repetir formato de fechas, moneda,
   estados y mensajes en cada tabla.
   ============================================================ */
function setText(id, value) {
    document.getElementById(id).textContent = value;
}
function money(value) {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(Number(value || 0));
}
function dateTime(value) {
    if (!value) return '';
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
}
function labelEnum(value = '') {
    return String(value).replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, letter => letter.toUpperCase());
}
function status(value) {
    return `<span class="status ${value}">${labelEnum(value)}</span>`;
}
function emptyRow(columns, message) {
    return `<tr><td colspan="${columns}" class="empty-row">${escapeHtml(message)}</td></tr>`;
}
function detailItem(label, value) {
    return `<div class="detail-item"><small>${escapeHtml(label)}</small><strong>${escapeHtml(String(value ?? ''))}</strong></div>`;
}
function edgeKey(a, b) {
    return [a, b].sort((x, y) => x.localeCompare(y)).join('|');
}
function splitNodeName(name) {
    if (name.length < 13) return [name];
    const parts = name.split(' ');
    const middle = Math.ceil(parts.length / 2);
    return [parts.slice(0, middle).join(' '), parts.slice(middle).join(' ')];
}
function prettyJson(raw) {
    try {
        const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
        const text = JSON.stringify(parsed, null, 2);
        return text.length > 500 ? text.slice(0, 500) + '\n…' : text;
    } catch {
        return String(raw || '');
    }
}
function escapeHtml(value = '') {
    return String(value).replace(/[&<>'"]/g, char => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    })[char]);
}
function toast(title, message = '', isError = false) {
    const element = document.getElementById('toast');
    document.getElementById('toastTitle').textContent = title;
    document.getElementById('toastMessage').textContent = message;
    element.querySelector('.toast-icon').textContent = isError ? '!' : '✓';
    element.style.background = isError ? '#7d3041' : '#18375f';
    element.classList.add('show');
    clearTimeout(window.__toastTimer);
    window.__toastTimer = setTimeout(() => element.classList.remove('show'), 3500);
}

/* Los buscadores vuelven a pintar su tabla sin consultar nuevamente al servidor. */
document.getElementById('productSearch').addEventListener('input', renderProducts);
document.getElementById('clientSearch').addEventListener('input', renderClients);
document.getElementById('orderSearch').addEventListener('input', renderOrders);

// Primera carga del sistema.
loadAll();
