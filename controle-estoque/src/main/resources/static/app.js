// ======================================================================
// Configuracao das telas: cada entidade lista seus campos.
// tipo "select" = relacionamento (busca as opcoes em outra rota da API)
// ======================================================================
const ENTIDADES = {
    produtos: {
        titulo: 'Produtos', item: 'produto', novo: 'Novo produto', icone: '□', csv: 'produtos.csv',
        campos: [
            { nome: 'nome', label: 'Nome', coluna: 'Produto' },
            { nome: 'preco', label: 'Preço (R$)', tipo: 'number' },
            { nome: 'categoria', label: 'Categoria', tipo: 'select', origem: 'categorias' },
            { nome: 'fornecedor', label: 'Fornecedor', tipo: 'select', origem: 'fornecedores' }
        ],
        extras: [{ nome: 'quantidadeEstoque', label: 'Estoque' }],
        ordemColunas: ['nome', 'categoria', 'fornecedor', 'preco', 'quantidadeEstoque']
    },
    entradas: {
        titulo: 'Entradas (compras)', menu: 'Entradas', item: 'entrada', novo: 'Nova entrada', icone: '⇩',
        campos: [
            { nome: 'produto', label: 'Produto', tipo: 'select', origem: 'produtos' },
            { nome: 'quantidade', label: 'Quantidade', tipo: 'number', inteiro: true },
            { nome: 'data', label: 'Data', tipo: 'date', opcional: true }
        ]
    },
    saidas: {
        titulo: 'Saídas (vendas)', menu: 'Saídas', item: 'saída', novo: 'Nova saída', icone: '⇧',
        campos: [
            { nome: 'produto', label: 'Produto', tipo: 'select', origem: 'produtos' },
            { nome: 'cliente', label: 'Cliente', tipo: 'select', origem: 'clientes' },
            { nome: 'quantidade', label: 'Quantidade', tipo: 'number', inteiro: true },
            { nome: 'data', label: 'Data', tipo: 'date', opcional: true }
        ]
    },
    categorias: {
        titulo: 'Categorias', item: 'categoria', novo: 'Nova categoria', icone: '◇',
        campos: [{ nome: 'nome', label: 'Nome' }]
    },
    fornecedores: {
        titulo: 'Fornecedores', item: 'fornecedor', novo: 'Novo fornecedor', icone: '▤',
        campos: [
            { nome: 'nome', label: 'Nome' },
            { nome: 'cnpj', label: 'CNPJ' },
            { nome: 'cep', label: 'CEP', tipo: 'cep', opcional: true },
            { nome: 'endereco', label: 'Endereço', opcional: true },
            { nome: 'cidade', label: 'Cidade', opcional: true },
            { nome: 'uf', label: 'UF', opcional: true }
        ]
    },
    clientes: {
        titulo: 'Clientes', item: 'cliente', novo: 'Novo cliente', icone: '♧', csv: 'clientes.csv',
        campos: [
            { nome: 'nome', label: 'Nome' },
            { nome: 'cpf', label: 'CPF', mascara: mascaraCpf, placeholder: 'Somente números, sem pontos ou traços.' },
            { nome: 'telefone', label: 'Telefone', mascara: mascaraTelefone, placeholder: 'Exemplo: 119999999999', opcional: true },
            { nome: 'cep', label: 'CEP', tipo: 'cep', placeholder: 'Somente números', opcional: true },
            { nome: 'endereco', label: 'Endereço', opcional: true },
            { nome: 'cidade', label: 'Cidade', opcional: true },
            { nome: 'uf', label: 'UF', opcional: true }
        ]
    },
    usuarios: {
        titulo: 'Usuários', item: 'usuário', novo: 'Novo usuário', icone: '♙',
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
let registros = [];
let versaoTela = 0;
let versaoPainel = 0;
let retornoFoco = null;
let salvando = false;
const ESTOQUE_MINIMO = 5;
const porId = id => document.getElementById(id);
const normalizar = valor => String(exibir(valor)).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

// ---------------- Chamadas a API ----------------
async function api(metodo, url, corpo) {
    const resp = await fetch(url, {
        method: metodo,
        headers: { 'Content-Type': 'application/json' },
        body: corpo ? JSON.stringify(corpo) : undefined
    });
    if (resp.status === 401) location.href = '/login.html';
    if (resp.status === 403) throw new Error('Acesso negado: somente ADMIN.');
    if (!resp.ok) {
        const erro = await resp.json().catch(() => ({}));
        throw new Error(erro.erro || 'Erro ' + resp.status);
    }
    return resp.status === 204 ? null : resp.json();
}

function mostrarMensagem(texto = '', erro = false) {
    const mensagem = porId('mensagem');
    mensagem.textContent = texto;
    mensagem.hidden = !texto;
    mensagem.classList.toggle('error', erro);
}

function mensagemFormulario(texto = '') {
    const mensagem = porId('erro-formulario');
    mensagem.textContent = texto;
    mensagem.hidden = !texto;
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

function mascaraCpf(digitos) {
    return digitos.slice(0, 11)
        .replace(/^(\d{3})(?=\d)/, '$1.')
        .replace(/^(\d{3}\.\d{3})(?=\d)/, '$1.')
        .replace(/^(\d{3}\.\d{3}\.\d{3})(?=\d)/, '$1-');
}

function mascaraTelefone(digitos) {
    const ddd = digitos.slice(0, 2);
    const numero = digitos.slice(2, 11);
    const prefixo = numero.length === 9 ? 5 : 4;
    if (!ddd) return '';
    if (!numero) return '(' + ddd;
    return '(' + ddd + ') ' + numero.slice(0, prefixo) +
        (numero.length > prefixo ? '-' + numero.slice(prefixo) : '');
}

function limitarInsercao(evento, mascara) {
    const campo = evento.target;
    if (!evento.data || campo.selectionEnd === campo.value.length) return;

    const valor = campo.value.slice(0, campo.selectionStart) + evento.data + campo.value.slice(campo.selectionEnd);
    const digitos = valor.replace(/\D/g, '');
    // Evita descartar o ultimo digito ao inserir texto no meio de um campo cheio.
    if (mascara(digitos).replace(/\D/g, '').length < digitos.length) evento.preventDefault();
}

function aplicarMascara(evento, mascara) {
    const campo = evento.target;
    const valor = campo.value;
    const formatado = mascara(valor.replace(/\D/g, ''));
    if (valor === formatado) return;

    let posicao = formatado.length;
    if (campo.selectionStart < valor.length) {
        const digitosAntes = valor.slice(0, campo.selectionStart).replace(/\D/g, '').length;
        posicao = 0;
        let digitos = 0;
        while (posicao < formatado.length && digitos < digitosAntes) {
            if (/\d/.test(formatado[posicao])) digitos++;
            posicao++;
        }
        if (evento.inputType === 'deleteContentForward') {
            while (posicao < formatado.length && /\D/.test(formatado[posicao])) posicao++;
        }
    }
    campo.value = formatado;
    campo.setSelectionRange(posicao, posicao);
}

// ---------------- Telas ----------------
function botao(texto, classe, acao) {
    const elemento = document.createElement('button');
    elemento.type = 'button';
    elemento.className = classe;
    elemento.textContent = texto;
    elemento.addEventListener('click', acao);
    return elemento;
}

async function abrir(nome) {
    if (salvando || atual === nome) return;
    fecharPainel(false);
    atual = nome;
    const versao = ++versaoTela;
    registros = [];
    mostrarMensagem();
    for (const item of porId('menu').children) {
        if (item.dataset.tela === nome) item.setAttribute('aria-current', 'page');
        else item.removeAttribute('aria-current');
    }
    // O Painel nao tem tabela nem cadastro: a area de trabalho da lugar ao resumo (painel.js)
    const ehPainel = nome === TELA_PAINEL;
    for (const id of ['acoes-titulo', 'filtros', 'caixa-tabela']) porId(id).hidden = ehPainel;
    porId('visao-painel').hidden = !ehPainel;
    if (ehPainel) return abrirPainelDoEstoque(versao);
    const cfg = ENTIDADES[nome];
    porId('titulo').textContent = cfg.titulo;
    porId('novo').textContent = '+ ' + cfg.novo;
    porId('novo').disabled = false;
    porId('baixar-csv').hidden = !cfg.csv;
    porId('busca').value = '';
    porId('busca').placeholder = 'Buscar ' + cfg.item;
    porId('rotulo-busca').textContent = porId('busca').placeholder;
    const filtro = porId('filtro-categoria');
    filtro.hidden = nome !== 'produtos';
    filtro.replaceChildren(new Option('Todas as categorias', ''));
    renderizarTabela('Carregando registros…');
    try {
        const [dados, categorias] = await Promise.all([
            api('GET', '/api/' + nome),
            nome === 'produtos' ? api('GET', '/api/categorias') : []
        ]);
        if (versao !== versaoTela) return;
        registros = dados;
        filtro.append(...categorias.map(c => new Option(c.nome, c.id)));
        renderizarTabela();
    } catch (erro) {
        if (versao !== versaoTela) return;
        renderizarTabela('Não foi possível carregar os registros.');
        mostrarMensagem(erro.message, true);
    }
}

function colunasTabela() {
    const cfg = ENTIDADES[atual];
    const campos = [...cfg.campos.filter(c => !c.oculto), ...(cfg.extras || [])];
    const colunas = cfg.ordemColunas
        ? cfg.ordemColunas.map(nome => campos.find(c => c.nome === nome))
        : campos;
    return [{ nome: 'id', label: 'ID' }, ...colunas];
}

function registrosVisiveis(colunas) {
    const busca = normalizar(porId('busca').value);
    const categoria = porId('filtro-categoria').value;
    return registros.filter(registro =>
        (atual !== 'produtos' || !categoria || String(registro.categoria?.id) === categoria) &&
        (!busca || colunas.some(c => normalizar(formatar(c.nome, registro[c.nome])).includes(busca)))
    );
}

function renderizarTabela(mensagemVazia) {
    const colunas = colunasTabela();
    const dados = registrosVisiveis(colunas);
    const cabecalho = document.createElement('tr');
    for (const titulo of [...colunas.map(c => c.coluna || c.label), 'Ações']) {
        const th = document.createElement('th');
        th.scope = 'col';
        th.textContent = titulo;
        cabecalho.append(th);
    }
    porId('cabecalho').replaceChildren(cabecalho);
    const corpo = porId('linhas');
    corpo.replaceChildren();
    porId('baixar-csv').disabled = dados.length === 0;
    for (const registro of dados) {
        const linha = corpo.insertRow();
        linha.classList.toggle('no-stock', atual === 'produtos' && registro.quantidadeEstoque === 0);
        for (const coluna of colunas) {
            const celula = linha.insertCell();
            const valor = registro[coluna.nome];
            celula.textContent = formatar(coluna.nome, valor) || (valor === 0 ? '0' : '—');
            if (coluna.nome === 'quantidadeEstoque' && valor < ESTOQUE_MINIMO) {
                const etiqueta = document.createElement('span');
                etiqueta.className = 'tag ' + (valor === 0 ? 'stock-warning' : 'stock-low');
                etiqueta.textContent = valor === 0 ? 'Sem estoque' : 'Estoque baixo';
                celula.append(etiqueta);
            }
        }
        const acoes = document.createElement('div');
        acoes.className = 'actions';
        acoes.append(botao('Editar', 'btn', evento => abrirPainel(registro.id, evento.currentTarget)));
        if (usuarioLogado.perfil === 'ADMIN') {
            acoes.append(botao('Excluir', 'btn danger', evento => excluir(registro.id, evento.currentTarget)));
        }
        linha.insertCell().append(acoes);
    }
    if (!dados.length) {
        const celula = corpo.insertRow().insertCell();
        celula.colSpan = colunas.length + 1;
        celula.className = 'empty';
        celula.textContent = mensagemVazia || (porId('busca').value.trim() || porId('filtro-categoria').value
            ? 'Nenhum resultado para esta busca.' : 'Nenhum registro cadastrado.');
    }
}

async function recarregarTabela(tela, versao) {
    const dados = await api('GET', '/api/' + tela);
    if (versao !== versaoTela) return;
    registros = dados;
    renderizarTabela();
}

function celulaCsv(valor) {
    let texto = String(valor ?? '');
    // Impede que texto de cadastro seja interpretado como fórmula ao abrir a planilha.
    if (typeof valor === 'string' && (/^\s*[=+\-@]/.test(texto) || /^[\t\r\n]/.test(texto))) texto = "'" + texto;
    return '"' + texto.replaceAll('"', '""') + '"';
}

function baixarCsv() {
    const arquivo = ENTIDADES[atual].csv;
    const colunas = colunasTabela();
    const dados = registrosVisiveis(colunas);
    if (!arquivo || !dados.length) return;
    const linhas = [colunas.map(c => c.coluna || c.label), ...dados.map(registro => colunas.map(c =>
        c.nome === 'preco' && registro.preco != null
            ? Number(registro.preco).toFixed(2).replace('.', ',')
            : exibir(registro[c.nome])
    ))];
    const csv = linhas.map(linha => linha.map(celulaCsv).join(';')).join('\r\n') + '\r\n';
    const url = URL.createObjectURL(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = arquivo;
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// ---------------- Formulário lateral ----------------
function criarCampo(c, registro, opcoes, tela, versao) {
    const label = document.createElement('label');
    label.textContent = c.nome === 'senha' && !registro ? 'Senha' : c.label;
    const campo = document.createElement(c.tipo === 'select' ? 'select' : 'input');
    campo.name = c.nome;
    campo.required = !c.opcional || (c.nome === 'senha' && !registro);
    const valor = c.oculto ? '' : registro?.[c.nome];
    if (c.tipo === 'select') {
        campo.append(new Option('Selecione…', ''));
        for (const opcao of c.opcoes || opcoes) {
            const estoque = tela === 'saidas' && c.origem === 'produtos' ? ' (estoque: ' + opcao.quantidadeEstoque + ')' : '';
            campo.append(new Option(exibir(opcao) + estoque, c.opcoes ? opcao : opcao.id));
        }
    } else {
        campo.type = c.tipo === 'cep' ? 'text' : (c.tipo || 'text');
        if (campo.type === 'password') campo.autocomplete = 'new-password';
        if (campo.type === 'number') {
            // quantidade e contada em unidades inteiras; o preco aceita centavos
            campo.min = c.inteiro ? '1' : '0';
            campo.step = c.inteiro ? '1' : 'any';
        }
        if (c.placeholder || c.tipo === 'cep') campo.placeholder = c.placeholder || 'Somente números';
        if (c.tipo === 'cep') campo.addEventListener('blur', () => buscarCep(campo, versao));
        if (c.mascara) {
            campo.inputMode = 'numeric';
            campo.onbeforeinput = evento => limitarInsercao(evento, c.mascara);
            campo.oninput = evento => aplicarMascara(evento, c.mascara);
        }
    }
    campo.value = valor && typeof valor === 'object' ? valor.id : (valor ?? '');
    if (c.mascara && registro) {
        campo.dataset.valorOriginal = campo.value;
        if (/^\d+$/.test(campo.value)) {
            const formatado = c.mascara(campo.value);
            if (formatado.replace(/\D/g, '') === campo.value) campo.value = formatado;
        }
        campo.defaultValue = campo.value;
    }
    label.append(campo);
    return label;
}

async function abrirPainel(id, acionador) {
    if (salvando) return;
    mostrarMensagem();
    const tela = atual;
    const cfg = ENTIDADES[tela];
    const versao = ++versaoPainel;
    editandoId = id;
    retornoFoco = acionador;
    porId('titulo-painel').textContent = id === null ? cfg.novo : 'Editar ' + cfg.item;
    porId('salvar').textContent = id === null ? 'Salvar ' + cfg.item : 'Salvar alterações';
    porId('salvar').disabled = true;
    porId('campos').replaceChildren();
    mensagemFormulario();
    porId('painel').hidden = false;
    porId('workspace').classList.add('open');
    porId('novo').setAttribute('aria-expanded', 'true');
    porId('fechar').focus();
    try {
        const [registro, opcoes] = await Promise.all([
            id === null ? null : api('GET', `/api/${tela}/${id}`),
            Promise.all(cfg.campos.map(c => c.origem ? api('GET', '/api/' + c.origem) : []))
        ]);
        if (versao !== versaoPainel) return;
        porId('campos').replaceChildren(...cfg.campos.map((c, i) => criarCampo(c, registro, opcoes[i], tela, versao)));
        porId('salvar').disabled = false;
        porId('campos').querySelector('input, select').focus();
    } catch (erro) {
        if (versao === versaoPainel) mensagemFormulario(erro.message);
    }
}

function fecharPainel(restaurarFoco = true) {
    if (porId('painel').hidden || salvando) return;
    versaoPainel++;
    editandoId = null;
    porId('painel').hidden = true;
    porId('workspace').classList.remove('open');
    porId('novo').setAttribute('aria-expanded', 'false');
    if (restaurarFoco) (retornoFoco?.isConnected ? retornoFoco : porId('novo')).focus();
    retornoFoco = null;
}

async function salvar(evento) {
    evento.preventDefault();
    const botaoSalvar = porId('salvar');
    if (botaoSalvar.disabled) return;
    salvando = true;
    botaoSalvar.disabled = true;
    mensagemFormulario();
    const tela = atual;
    const versao = versaoTela;
    const dados = {};
    for (const c of ENTIDADES[tela].campos) {
        const campo = evento.currentTarget.elements[c.nome];
        let valor = campo.value;
        // Preserva o valor gravado se a máscara só mudou sua exibição na edição.
        if (c.mascara && valor === campo.defaultValue) valor = campo.dataset.valorOriginal ?? valor;
        if (valor === '') valor = null;
        else if (c.origem) valor = { id: Number(valor) };
        else if (c.tipo === 'number') valor = Number(valor);
        dados[c.nome] = valor;
    }
    try {
        if (editandoId !== null) await api('PUT', `/api/${tela}/${editandoId}`, dados);
        else await api('POST', '/api/' + tela, dados);
    } catch (erro) {
        mensagemFormulario(erro.message);
        botaoSalvar.disabled = false;
        return;
    } finally {
        salvando = false;
    }
    fecharPainel(false);
    porId('novo').focus();
    try {
        await recarregarTabela(tela, versao);
        if (versao === versaoTela) mostrarMensagem('Registro salvo com sucesso!');
    } catch (erro) {
        if (versao === versaoTela) mostrarMensagem('Registro salvo, mas a lista não pôde ser atualizada. ' + erro.message, true);
    }
}

async function excluir(id, acionador) {
    if (salvando || !confirm('Excluir o registro ' + id + '?')) return;
    mostrarMensagem();
    const tela = atual;
    const versao = versaoTela;
    acionador.disabled = true;
    try {
        await api('DELETE', `/api/${tela}/${id}`);
        if (versao !== versaoTela) return;
        if (id === editandoId) fecharPainel();
        await recarregarTabela(tela, versao);
        if (versao === versaoTela) mostrarMensagem('Registro excluído com sucesso!');
    } catch (erro) {
        if (versao === versaoTela) mostrarMensagem(erro.message, true);
    } finally {
        acionador.disabled = false;
    }
}

async function buscarCep(campo, versao) {
    const cep = campo.value.trim();
    if (!cep) return;
    try {
        const endereco = await api('GET', '/api/cep/' + encodeURIComponent(cep));
        if (versao !== versaoPainel || campo.value.trim() !== cep || salvando) return;
        for (const chave of ['cep', 'endereco', 'cidade', 'uf']) {
            campo.form.elements[chave].value = endereco[chave] ?? '';
        }
        mensagemFormulario();
    } catch (erro) {
        if (versao === versaoPainel && campo.value.trim() === cep && !salvando) mensagemFormulario(erro.message);
    }
}

async function sair() {
    await fetch('/logout', { method: 'POST' });
    location.href = '/login.html';
}

async function iniciar() {
    usuarioLogado = await api('GET', '/api/usuarios/me');
    porId('usuario').textContent = usuarioLogado.login + ' (' + usuarioLogado.perfil + ')';
    for (const [nome, cfg] of [[TELA_PAINEL, MENU_PAINEL], ...Object.entries(ENTIDADES)]) {
        if (cfg.somenteAdmin && usuarioLogado.perfil !== 'ADMIN') continue;
        const item = botao(cfg.menu || cfg.titulo, '', () => abrir(nome));
        item.dataset.tela = nome;
        const icone = document.createElement('span');
        icone.className = 'nav-icon';
        icone.setAttribute('aria-hidden', 'true');
        icone.textContent = cfg.icone;
        item.prepend(icone);
        porId('menu').append(item);
    }
    porId('novo').onclick = evento => abrirPainel(null, evento.currentTarget);
    porId('fechar').onclick = porId('cancelar').onclick = () => fecharPainel();
    porId('baixar-csv').onclick = baixarCsv;
    porId('busca').oninput = () => renderizarTabela();
    porId('filtro-categoria').onchange = () => renderizarTabela();
    porId('formulario').onsubmit = salvar;
    porId('sair').onclick = () => sair().catch(erro => mostrarMensagem(erro.message, true));
    document.addEventListener('keydown', evento => {
        if (evento.key === 'Escape') fecharPainel();
    });
    await abrir(TELA_PAINEL);
}

iniciar().catch(erro => mostrarMensagem(erro.message, true));
