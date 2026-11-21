// ======================================================================
// Configuracao das telas: cada entidade lista seus campos.
// tipo "select" = relacionamento (busca as opcoes em outra rota da API)
// ======================================================================
const ENTIDADES = {
    produtos: {
        titulo: 'Produtos',
        campos: [
            { nome: 'nome', label: 'Nome' },
            { nome: 'preco', label: 'Preco (R$)', tipo: 'number' },
            { nome: 'categoria', label: 'Categoria', tipo: 'select', origem: 'categorias' },
            { nome: 'fornecedor', label: 'Fornecedor', tipo: 'select', origem: 'fornecedores' }
        ],
        extras: [{ nome: 'quantidadeEstoque', label: 'Estoque' }]
    },
    entradas: {
        titulo: 'Entradas (compras)',
        campos: [
            { nome: 'produto', label: 'Produto', tipo: 'select', origem: 'produtos' },
            { nome: 'quantidade', label: 'Quantidade', tipo: 'number' },
            { nome: 'data', label: 'Data', tipo: 'date', opcional: true }
        ]
    },
    saidas: {
        titulo: 'Saidas (vendas)',
        campos: [
            { nome: 'produto', label: 'Produto', tipo: 'select', origem: 'produtos' },
            { nome: 'cliente', label: 'Cliente', tipo: 'select', origem: 'clientes' },
            { nome: 'quantidade', label: 'Quantidade', tipo: 'number' },
            { nome: 'data', label: 'Data', tipo: 'date', opcional: true }
        ]
    },
    categorias: {
        titulo: 'Categorias',
        campos: [{ nome: 'nome', label: 'Nome' }]
    },
    fornecedores: {
        titulo: 'Fornecedores',
        campos: [
            { nome: 'nome', label: 'Nome' },
            { nome: 'cnpj', label: 'CNPJ' },
            { nome: 'cep', label: 'CEP', tipo: 'cep', opcional: true },
            { nome: 'endereco', label: 'Endereco', opcional: true },
            { nome: 'cidade', label: 'Cidade', opcional: true },
            { nome: 'uf', label: 'UF', opcional: true }
        ]
    },
    clientes: {
        titulo: 'Clientes',
        campos: [
            { nome: 'nome', label: 'Nome' },
            { nome: 'cpf', label: 'CPF' },
            { nome: 'telefone', label: 'Telefone', opcional: true },
            { nome: 'cep', label: 'CEP', tipo: 'cep', opcional: true },
            { nome: 'endereco', label: 'Endereco', opcional: true },
            { nome: 'cidade', label: 'Cidade', opcional: true },
            { nome: 'uf', label: 'UF', opcional: true }
        ]
    },
    usuarios: {
        titulo: 'Usuarios',
        somenteAdmin: true,
        campos: [
            { nome: 'login', label: 'Login' },
            { nome: 'senha', label: 'Senha (em branco = manter)', tipo: 'password', opcional: true, oculto: true },
            { nome: 'perfil', label: 'Perfil', tipo: 'select', opcoes: ['USER', 'ADMIN'] }
        ]
    }
};

let usuarioLogado = null;
let atual = null;        
let editandoId = null;
const ESTOQUE_MINIMO = 5;  

// ---------------- Chamadas a API ----------------
async function api(metodo, url, corpo) {
    const resp = await fetch(url, {
        method: metodo,
        headers: { 'Content-Type': 'application/json' },
        body: corpo ? JSON.stringify(corpo) : undefined
    });
    if (resp.status === 401) { location.href = '/login.html'; return; }
    if (resp.status === 403) throw new Error('Acesso negado: somente ADMIN.');
    if (!resp.ok) {
        const erro = await resp.json().catch(() => ({}));
        throw new Error(erro.erro || 'Erro ' + resp.status);
    }
    return resp.status === 204 ? null : resp.json();
}

function mostrarErro(texto) {
    const p = document.getElementById('mensagem');
    p.textContent = texto || '';
    p.hidden = !texto;
}


function exibir(valor) {
    if (valor === null || valor === undefined) return '';
    if (typeof valor === 'object') return valor.nome || valor.login || valor.id;
    return valor;
}


function formatar(campo, valor) {
    if (campo === 'preco') {
        return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    }
    return exibir(valor);
}
// ---------------- Telas ----------------
async function abrir(nome) {
    atual = nome;
    editandoId = null;
    mostrarErro();
    document.getElementById('titulo').textContent = ENTIDADES[nome].titulo;
    await montarFormulario();
    await carregarTabela();
}

async function carregarTabela() {
    const cfg = ENTIDADES[atual];
    const colunas = [{ nome: 'id', label: 'ID' }, ...cfg.campos.filter(c => !c.oculto), ...(cfg.extras || [])];

    document.getElementById('cabecalho').innerHTML =
        '<tr>' + colunas.map(c => `<th>${c.label}</th>`).join('') + '<th>Acoes</th></tr>';

    const registros = await api('GET', '/api/' + atual);
    const admin = usuarioLogado.perfil === 'ADMIN';

    document.getElementById('linhas').innerHTML = registros.map(r =>
        '<tr' + (atual === 'produtos' && r.quantidadeEstoque < ESTOQUE_MINIMO ? ' style="background:#ffebee"' : '') + '>' +
        colunas.map(c => `<td>${formatar(c.nome, r[c.nome])}</td>`).join('') +
        `<td><button onclick="editar(${r.id})">Editar</button>
             ${admin ? `<button class="perigo" onclick="excluir(${r.id})">Excluir</button>` : ''}</td></tr>`
    ).join('');
}

async function montarFormulario() {
    const cfg = ENTIDADES[atual];
    let html = '';
    for (const c of cfg.campos) {
        const req = c.opcional ? '' : 'required';
        if (c.tipo === 'select') {
            // opcoes fixas (perfil) ou vindas da API (relacionamento)
            const comEstoque = atual === 'saidas' && c.origem === 'produtos';
            const opcoes = c.opcoes
                ? c.opcoes.map(o => `<option value="${o}">${o}</option>`)
                : (await api('GET', '/api/' + c.origem)).map(o => `<option value="${o.id}">${exibir(o)}${comEstoque ? ' (estoque: ' + o.quantidadeEstoque + ')' : ''}</option>`);
            html += `<label>${c.label}<select name="${c.nome}" ${req}><option value="">--</option>${opcoes.join('')}</select></label>`;
        } else {
            const tipo = c.tipo === 'cep' ? 'text' : (c.tipo || 'text');
            const extra = c.tipo === 'number' ? 'step="any" min="0"' : '';
            const cep = c.tipo === 'cep' ? 'onblur="buscarCep(this.value)" placeholder="so numeros"' : '';
            html += `<label>${c.label}<input name="${c.nome}" type="${tipo}" ${extra} ${cep} ${req}></label>`;
        }
    }
    html += `<button type="submit">Salvar</button> <button type="button" onclick="abrir(atual)">Limpar</button>`;
    const form = document.getElementById('formulario');
    form.innerHTML = html;
    form.onsubmit = salvar;
}

// ---------------- Acoes ----------------
async function salvar(evento) {
    evento.preventDefault();
    const form = evento.target;
    const dados = {};
    for (const c of ENTIDADES[atual].campos) {
        let valor = form.elements[c.nome].value;
        if (valor === '') valor = null;
        else if (c.tipo === 'select' && c.origem) valor = { id: Number(valor) }; // relacionamento -> {id}
        else if (c.tipo === 'number') valor = Number(valor);
        dados[c.nome] = valor;
    }
    try {
        if (editandoId) await api('PUT', `/api/${atual}/${editandoId}`, dados);
        else await api('POST', '/api/' + atual, dados);
        await abrir(atual);
    } catch (e) {
        mostrarErro(e.message);
    }
}

async function editar(id) {
    const registro = await api('GET', `/api/${atual}/${id}`);
    const form = document.getElementById('formulario');
    for (const c of ENTIDADES[atual].campos) {
        const valor = registro[c.nome];
        form.elements[c.nome].value = valor && typeof valor === 'object' ? valor.id : (valor ?? '');
    }
    editandoId = id;
    mostrarErro();
}

async function excluir(id) {
    if (!confirm('Excluir o registro ' + id + '?')) return;
    try {
        await api('DELETE', `/api/${atual}/${id}`);
        await carregarTabela();
        mostrarErro();
    } catch (e) {
        mostrarErro(e.message);
    }
}

// Integracao com ViaCEP (chama o nosso backend, que chama o ViaCEP)
async function buscarCep(cep) {
    if (!cep) return;
    try {
        const end = await api('GET', '/api/cep/' + cep);
        const form = document.getElementById('formulario');
        form.elements.cep.value = end.cep;
        form.elements.endereco.value = end.endereco;
        form.elements.cidade.value = end.cidade;
        form.elements.uf.value = end.uf;
        mostrarErro();
    } catch (e) {
        mostrarErro(e.message);
    }
}

async function sair() {
    await fetch('/logout', { method: 'POST' });
    location.href = '/login.html';
}

// ---------------- Inicio ----------------
async function iniciar() {
    usuarioLogado = await api('GET', '/api/usuarios/me');
    document.getElementById('usuario').textContent = usuarioLogado.login + ' (' + usuarioLogado.perfil + ')';

    document.getElementById('menu').innerHTML = Object.entries(ENTIDADES)
        .filter(([, cfg]) => !cfg.somenteAdmin || usuarioLogado.perfil === 'ADMIN')
        .map(([nome, cfg]) => `<button onclick="abrir('${nome}')">${cfg.titulo}</button>`)
        .join('');

    abrir('produtos');
}

iniciar();
