// ======================================================================
// Tela Painel: resumo do estoque, lido de GET /api/painel.
// Carregado antes do app.js; usa api(), porId(), formatar() e mostrarMensagem() de la.
// ======================================================================
const TELA_PAINEL = 'painel';
const MENU_PAINEL = { titulo: 'Painel', icone: '▦' };

// As funcoes de apoio ficam dentro desta funcao para nao disputar nome com as do app.js
// (os dois arquivos dividem o mesmo escopo global). So abrirPainelDoEstoque sai daqui.
const abrirPainelDoEstoque = (() => {

function criar(tag, classe, texto) {
    const elemento = document.createElement(tag);
    if (classe) elemento.className = classe;
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
}

const inteiro = valor => Number(valor).toLocaleString('pt-BR');

// O "i" ao lado de um titulo: a explicacao fica num balao que abre ao passar o mouse,
// tocar ou focar pelo teclado. Na pagina ficam so o titulo e o dado.
function info(texto) {
    const icone = criar('span', 'info', 'i');
    icone.tabIndex = 0;
    icone.dataset.info = texto;
    icone.setAttribute('role', 'note');
    icone.setAttribute('aria-label', texto);
    // perto da borda direita o balao abre para a esquerda, para nao sair da tela
    const virar = () => icone.classList.toggle('info-fim', icone.getBoundingClientRect().left + 270 > innerWidth);
    icone.addEventListener('mouseenter', virar);
    icone.addEventListener('focus', virar);
    return icone;
}

// O "i" fica ao lado do titulo, e nao dentro dele, para o leitor de tela nao ler "Valor em estoque i"
function titulo(tag, texto, explicacao) {
    const linha = criar('div', 'linha-titulo');
    linha.append(criar(tag, '', texto));
    if (explicacao) linha.append(info(explicacao));
    return linha;
}

function cartao(nome, valor, explicacao) {
    const caixa = criar('article', 'kpi');
    caixa.append(titulo('h3', nome, explicacao), criar('p', 'kpi-valor', valor));
    return caixa;
}

function quadro(nome, explicacao, conteudo, largo) {
    const secao = criar('section', 'quadro' + (largo ? ' quadro-largo' : ''));
    secao.append(titulo('h2', nome, explicacao), conteudo);
    return secao;
}

function vazio(texto) {
    return criar('p', 'empty quadro-vazio', texto);
}

// Tabela simples: colunas = [{ titulo, numero, celula(linha) }]
function tabela(colunas, linhas) {
    const caixa = criar('div', 'table-box');
    const tabela = criar('table');
    const cabecalho = tabela.createTHead().insertRow();
    for (const coluna of colunas) {
        const th = criar('th', coluna.numero ? 'num' : '', coluna.titulo);
        th.scope = 'col';
        cabecalho.append(th);
    }
    const corpo = tabela.createTBody();
    for (const linha of linhas) {
        const tr = corpo.insertRow();
        for (const coluna of colunas) {
            const td = tr.insertCell();
            if (coluna.numero) td.className = 'num';
            td.append(coluna.celula(linha));
        }
    }
    caixa.append(tabela);
    return caixa;
}

// Uma barra por produto, proporcional ao mais vendido da lista
function barras(maisVendidos) {
    const maior = Math.max(...maisVendidos.map(item => item.quantidade));
    const lista = criar('ol', 'barras');
    for (const item of maisVendidos) {
        const linha = criar('li');
        const trilho = criar('span', 'trilho');
        const barra = criar('span', 'barra');
        barra.style.width = (item.quantidade / maior * 100) + '%';
        trilho.append(barra);
        linha.append(criar('span', 'barra-nome', item.produto),
            criar('span', 'num', inteiro(item.quantidade) + ' un.'), trilho);
        lista.append(linha);
    }
    return lista;
}

function etiquetaEstoque(produto) {
    const celula = document.createDocumentFragment();
    const zerado = produto.quantidadeEstoque === 0;
    celula.append(inteiro(produto.quantidadeEstoque),
        criar('span', 'tag ' + (zerado ? 'stock-warning' : 'stock-low'), zerado ? 'Sem estoque' : 'Estoque baixo'));
    return celula;
}

function montarPainel(p) {
    const dias = p.diasDoPeriodo;
    const cartoes = criar('div', 'kpis');
    cartoes.append(
        cartao('Produtos cadastrados', inteiro(p.totalProdutos)),
        cartao('Unidades em estoque', inteiro(p.unidadesEmEstoque)),
        cartao('Valor em estoque', formatar('preco', p.valorEmEstoque),
            'Soma do preço × quantidade em estoque de cada produto.'),
        cartao('Estoque baixo', inteiro(p.produtosEstoqueBaixo),
            'Produtos com menos de ' + p.estoqueMinimo + ' unidades, sem contar os zerados.'),
        cartao('Sem estoque', inteiro(p.produtosSemEstoque)),
        cartao('Entradas em ' + dias + ' dias', inteiro(p.unidadesEntradas),
            'Unidades compradas nos últimos ' + dias + ' dias, contando hoje, pela data da entrada.'),
        cartao('Saídas em ' + dias + ' dias', inteiro(p.unidadesSaidas),
            'Unidades vendidas nos últimos ' + dias + ' dias, contando hoje, pela data da saída.')
    );

    const quadros = criar('div', 'quadros');
    quadros.append(
        quadro('Mais vendidos',
            'Os cinco produtos com mais unidades vendidas nos últimos ' + dias + ' dias.',
            p.maisVendidos.length ? barras(p.maisVendidos) : vazio('Nenhuma saída nos últimos ' + dias + ' dias.')),
        quadro('Para repor',
            'Produtos com menos de ' + p.estoqueMinimo + ' unidades, do menor estoque para o maior.',
            p.paraRepor.length ? tabela([
                { titulo: 'Produto', celula: produto => produto.nome },
                { titulo: 'Fornecedor', celula: produto => produto.fornecedor.nome },
                { titulo: 'Estoque', celula: etiquetaEstoque }
            ], p.paraRepor) : vazio('Nenhum produto abaixo do mínimo.')),
        quadro('Estoque por categoria',
            'Só aparecem as categorias que têm produto cadastrado.',
            p.porCategoria.length ? tabela([
                { titulo: 'Categoria', celula: linha => linha.categoria },
                { titulo: 'Produtos', numero: true, celula: linha => inteiro(linha.produtos) },
                { titulo: 'Unidades', numero: true, celula: linha => inteiro(linha.unidades) },
                { titulo: 'Valor', numero: true, celula: linha => formatar('preco', linha.valor) }
            ], p.porCategoria) : vazio('Nenhum produto cadastrado.'), true)
    );
    return [cartoes, quadros];
}

// Chamado pelo abrir() do app.js quando o usuario entra no Painel
return async function (versao) {
    const visao = porId('visao-painel');
    porId('titulo').textContent = MENU_PAINEL.titulo;
    visao.replaceChildren(vazio('Carregando o resumo…'));
    try {
        const painel = await api('GET', '/api/painel');
        if (versao !== versaoTela) return;   // o usuario ja trocou de tela
        visao.replaceChildren(...montarPainel(painel));
    } catch (erro) {
        if (versao !== versaoTela) return;
        visao.replaceChildren(vazio('Não foi possível carregar o resumo.'));
        mostrarMensagem(erro.message, true);
    }
};

})();
