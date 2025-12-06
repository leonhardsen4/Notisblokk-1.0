/**
 * Gestão de Pessoas
 */

function pessoasApp() {
    return {
        // Estado
        pessoas: [],
        pessoasFiltradas: [],
        formularioAtivo: false,
        editando: false,
        termoPesquisa: '',
        pesquisaTimeout: null,
        mostrarControlesColunas: false,
        colunaOrdenacao: 'nome',
        direcaoOrdenacao: 'asc',

        // Controle de colunas visíveis
        colunasVisiveis: {
            nome: true,
            cpf: true,
            telefone: true,
            email: true
        },

        // Formulário
        form: {
            id: null,
            nome: '',
            cpf: '',
            telefone: '',
            email: '',
            observacoes: ''
        },

        /**
         * Inicialização
         */
        async init() {
            console.log('DEBUG_AUDIENCIAS: Inicializando gestão de pessoas...');
            await this.carregar();

            // Verificar se há parâmetro 'nome' na URL
            const urlParams = new URLSearchParams(window.location.search);
            const nomeParam = urlParams.get('nome');
            if (nomeParam) {
                // Abrir formulário e preencher nome
                this.abrirFormulario();
                this.form.nome = nomeParam;
                console.log('DEBUG_AUDIENCIAS: Nome pré-preenchido:', nomeParam);
            }
        },

        /**
         * Carregar pessoas
         */
        async carregar() {
            try {
                const res = await fetch('/api/audiencias/pessoas');
                const data = await res.json();

                if (data.success) {
                    this.pessoas = data.dados || [];
                    this.pessoasFiltradas = [...this.pessoas];
                    console.log(`DEBUG_AUDIENCIAS: ${this.pessoas.length} pessoas carregadas`);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar pessoas:', error);
                this.mostrarErro('Erro ao carregar pessoas');
            }
        },

        /**
         * Computed property: pessoas processadas (filtradas e ordenadas)
         */
        get pessoasProcessadas() {
            // 1. Filtrar por pesquisa
            let resultado = this.pessoasFiltradas;

            // 2. Ordenar
            resultado = [...resultado].sort((a, b) => {
                let valorA, valorB;

                switch (this.colunaOrdenacao) {
                    case 'nome':
                        valorA = (a.nome || '').toLowerCase();
                        valorB = (b.nome || '').toLowerCase();
                        break;
                    case 'cpf':
                        valorA = (a.cpf || '').toLowerCase();
                        valorB = (b.cpf || '').toLowerCase();
                        break;
                    case 'telefone':
                        valorA = (a.telefone || '').toLowerCase();
                        valorB = (b.telefone || '').toLowerCase();
                        break;
                    case 'email':
                        valorA = (a.email || '').toLowerCase();
                        valorB = (b.email || '').toLowerCase();
                        break;
                    default:
                        valorA = (a.nome || '').toLowerCase();
                        valorB = (b.nome || '').toLowerCase();
                        break;
                }

                if (valorA < valorB) return this.direcaoOrdenacao === 'asc' ? -1 : 1;
                if (valorA > valorB) return this.direcaoOrdenacao === 'asc' ? 1 : -1;
                return 0;
            });

            return resultado;
        },

        /**
         * Pesquisar
         */
        pesquisar() {
            clearTimeout(this.pesquisaTimeout);
            this.pesquisaTimeout = setTimeout(() => {
                const termo = this.termoPesquisa.toLowerCase();
                if (termo.trim()) {
                    this.pessoasFiltradas = this.pessoas.filter(p =>
                        p.nome?.toLowerCase().includes(termo) ||
                        p.cpf?.toLowerCase().includes(termo) ||
                        p.email?.toLowerCase().includes(termo)
                    );
                } else {
                    this.pessoasFiltradas = [...this.pessoas];
                }
            }, 300);
        },

        /**
         * Ordenar por coluna
         */
        ordenarPor(coluna) {
            if (this.colunaOrdenacao === coluna) {
                // Alternar direção
                this.direcaoOrdenacao = this.direcaoOrdenacao === 'asc' ? 'desc' : 'asc';
            } else {
                // Nova coluna, começar em ascendente
                this.colunaOrdenacao = coluna;
                this.direcaoOrdenacao = 'asc';
            }
        },

        /**
         * Obter ícone de ordenação
         */
        getSortIcon(coluna) {
            if (this.colunaOrdenacao !== coluna) return '⇅';
            return this.direcaoOrdenacao === 'asc' ? '↑' : '↓';
        },

        /**
         * Abrir formulário
         */
        abrirFormulario() {
            this.formularioAtivo = true;
            this.editando = false;
            this.form = {
                id: null,
                nome: '',
                cpf: '',
                telefone: '',
                email: '',
                observacoes: ''
            };
        },

        /**
         * Editar
         */
        editar(pessoa) {
            this.formularioAtivo = true;
            this.editando = true;
            this.form = {
                id: pessoa.id,
                nome: pessoa.nome,
                cpf: pessoa.cpf || '',
                telefone: pessoa.telefone || '',
                email: pessoa.email || '',
                observacoes: pessoa.observacoes || ''
            };
            window.scrollTo({ top: 0, behavior: 'smooth' });
        },

        /**
         * Cancelar
         */
        cancelar() {
            this.formularioAtivo = false;
            this.editando = false;
            this.form = {
                id: null,
                nome: '',
                cpf: '',
                telefone: '',
                email: '',
                observacoes: ''
            };
        },

        /**
         * Salvar
         */
        async salvar() {
            try {
                const metodo = this.editando ? 'PUT' : 'POST';
                const url = this.editando
                    ? `/api/audiencias/pessoas/${this.form.id}`
                    : '/api/audiencias/pessoas';

                const res = await fetch(url, {
                    method: metodo,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        nome: this.form.nome,
                        cpf: this.form.cpf || null,
                        telefone: this.form.telefone || null,
                        email: this.form.email || null,
                        observacoes: this.form.observacoes || null
                    })
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso(this.editando ? 'Pessoa atualizada!' : 'Pessoa cadastrada!');
                    await this.carregar();
                    this.cancelar();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao salvar pessoa:', error);
                this.mostrarErro('Erro ao salvar pessoa');
            }
        },

        /**
         * Deletar
         */
        async deletar(id) {
            if (!confirm('Deseja realmente deletar esta pessoa?')) return;

            try {
                const res = await fetch(`/api/audiencias/pessoas/${id}`, { method: 'DELETE' });
                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Pessoa deletada!');
                    await this.carregar();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao deletar pessoa:', error);
                this.mostrarErro('Erro ao deletar pessoa');
            }
        },

        /**
         * Notificações
         */
        mostrarSucesso(mensagem) {
            if (typeof Toastify !== 'undefined') {
                Toastify({
                    text: mensagem,
                    duration: 3000,
                    close: true,
                    gravity: "top",
                    position: "right",
                    backgroundColor: "#10B981",
                }).showToast();
            } else {
                alert(mensagem);
            }
        },

        mostrarErro(mensagem) {
            if (typeof Toastify !== 'undefined') {
                Toastify({
                    text: mensagem,
                    duration: 3000,
                    close: true,
                    gravity: "top",
                    position: "right",
                    backgroundColor: "#EF4444",
                }).showToast();
            } else {
                alert(mensagem);
            }
        }
    }
}
