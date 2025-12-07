/**
 * Gestão de Processos Judiciais
 */

function processosApp() {
    return {
        // Estado
        processos: [],
        processosFiltrados: [],
        varas: [],
        formularioAtivo: false,
        editando: false,
        termoPesquisa: '',
        pesquisaTimeout: null,
        mostrarControlesColunas: false,
        colunaOrdenacao: 'numeroProcesso',
        direcaoOrdenacao: 'asc',

        // Controle de colunas visíveis
        colunasVisiveis: {
            numero: true,
            vara: true,
            competencia: true,
            artigo: true,
            status: true,
            criadoEm: true
        },

        // Formulário
        form: {
            id: null,
            numeroProcesso: '',
            competencia: '',
            artigo: '',
            varaId: null,
            status: 'EM_ANDAMENTO',
            observacoes: ''
        },

        /**
         * Inicialização
         */
        async init() {
            console.log('Inicializando gestão de processos...');
            await this.carregarVaras();
            await this.carregar();
        },

        /**
         * Carregar varas para o dropdown
         */
        async carregarVaras() {
            try {
                const res = await fetch('/api/audiencias/varas');
                const data = await res.json();

                if (data.success) {
                    this.varas = data.dados || [];
                    console.log(`${this.varas.length} varas carregadas`);
                }
            } catch (error) {
                console.error('Erro ao carregar varas:', error);
                this.mostrarErro('Erro ao carregar varas');
            }
        },

        /**
         * Carregar processos
         */
        async carregar() {
            try {
                const res = await fetch('/api/processos');
                const data = await res.json();

                if (data.success) {
                    this.processos = data.dados || [];
                    this.processosFiltrados = [...this.processos];
                    console.log(`${this.processos.length} processos carregados`);
                }
            } catch (error) {
                console.error('Erro ao carregar processos:', error);
                this.mostrarErro('Erro ao carregar processos');
            }
        },

        /**
         * Computed property: processos processados (filtrados e ordenados)
         */
        get processosProcessados() {
            // 1. Filtrar por pesquisa
            let resultado = this.processosFiltrados;

            // 2. Ordenar
            resultado = [...resultado].sort((a, b) => {
                let valorA, valorB;

                // Navegação em propriedades aninhadas (ex: vara.nome)
                const getNestedValue = (obj, path) => {
                    return path.split('.').reduce((current, prop) => current?.[prop], obj);
                };

                valorA = getNestedValue(a, this.colunaOrdenacao) || '';
                valorB = getNestedValue(b, this.colunaOrdenacao) || '';

                // Converter para string lowercase para comparação
                if (typeof valorA === 'string') valorA = valorA.toLowerCase();
                if (typeof valorB === 'string') valorB = valorB.toLowerCase();

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
                    this.processosFiltrados = this.processos.filter(p =>
                        p.numeroProcesso?.toLowerCase().includes(termo) ||
                        p.vara?.nome?.toLowerCase().includes(termo) ||
                        p.competencia?.descricao?.toLowerCase().includes(termo) ||
                        p.artigo?.toLowerCase().includes(termo)
                    );
                } else {
                    this.processosFiltrados = [...this.processos];
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
                numeroProcesso: '',
                competencia: '',
                artigo: '',
                varaId: null,
                status: 'EM_ANDAMENTO',
                observacoes: ''
            };
        },

        /**
         * Editar
         */
        editar(processo) {
            this.formularioAtivo = true;
            this.editando = true;
            this.form = {
                id: processo.id,
                numeroProcesso: processo.numeroProcesso,
                competencia: processo.competencia?.name || '',
                artigo: processo.artigo || '',
                varaId: processo.vara?.id || null,
                status: processo.status?.name || 'EM_ANDAMENTO',
                observacoes: processo.observacoes || ''
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
                numeroProcesso: '',
                competencia: '',
                artigo: '',
                varaId: null,
                status: 'EM_ANDAMENTO',
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
                    ? `/api/processos/${this.form.id}`
                    : '/api/processos';

                const res = await fetch(url, {
                    method: metodo,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        numeroProcesso: this.form.numeroProcesso,
                        competencia: this.form.competencia,
                        artigo: this.form.artigo || null,
                        varaId: this.form.varaId,
                        status: this.form.status,
                        observacoes: this.form.observacoes || null
                    })
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso(this.editando ? 'Processo atualizado!' : 'Processo cadastrado!');
                    await this.carregar();
                    this.cancelar();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('Erro ao salvar processo:', error);
                this.mostrarErro('Erro ao salvar processo');
            }
        },

        /**
         * Deletar
         */
        async deletar(id) {
            if (!confirm('Deseja realmente deletar este processo? ATENÇÃO: Todas as audiências e participantes vinculados serão deletados!')) return;

            try {
                const res = await fetch(`/api/processos/${id}`, { method: 'DELETE' });
                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Processo deletado!');
                    await this.carregar();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('Erro ao deletar processo:', error);
                this.mostrarErro('Erro ao deletar processo');
            }
        },

        /**
         * Formatar data para exibição
         */
        formatarData(dataISO) {
            if (!dataISO) return '-';
            const data = new Date(dataISO);
            return data.toLocaleDateString('pt-BR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
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
