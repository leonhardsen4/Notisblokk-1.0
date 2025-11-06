/**
 * Gestão de Advogados
 */

function advogadosApp() {
    return {
        // Estado
        advogados: [],
        advogadosFiltrados: [],
        formularioAtivo: false,
        editando: false,
        termoPesquisa: '',
        pesquisaTimeout: null,

        // Formulário
        form: {
            id: null,
            nome: '',
            oab: '',
            telefone: '',
            email: '',
            observacoes: ''
        },

        /**
         * Inicialização
         */
        async init() {
            console.log('DEBUG_AUDIENCIAS: Inicializando gestão de advogados...');
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
         * Carregar advogados
         */
        async carregar() {
            try {
                const res = await fetch('/api/audiencias/advogados');
                const data = await res.json();

                if (data.success) {
                    this.advogados = data.dados || [];
                    this.advogadosFiltrados = [...this.advogados];
                    console.log(`DEBUG_AUDIENCIAS: ${this.advogados.length} advogados carregados`);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar advogados:', error);
                this.mostrarErro('Erro ao carregar advogados');
            }
        },

        /**
         * Pesquisar
         */
        pesquisar() {
            clearTimeout(this.pesquisaTimeout);
            this.pesquisaTimeout = setTimeout(() => {
                const termo = this.termoPesquisa.toLowerCase();
                if (termo.trim()) {
                    this.advogadosFiltrados = this.advogados.filter(a =>
                        a.nome?.toLowerCase().includes(termo) ||
                        a.oab?.toLowerCase().includes(termo) ||
                        a.email?.toLowerCase().includes(termo)
                    );
                } else {
                    this.advogadosFiltrados = [...this.advogados];
                }
            }, 300);
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
                oab: '',
                telefone: '',
                email: '',
                observacoes: ''
            };
        },

        /**
         * Editar
         */
        editar(advogado) {
            this.formularioAtivo = true;
            this.editando = true;
            this.form = {
                id: advogado.id,
                nome: advogado.nome,
                oab: advogado.oab,
                telefone: advogado.telefone || '',
                email: advogado.email || '',
                observacoes: advogado.observacoes || ''
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
                oab: '',
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
                    ? `/api/audiencias/advogados/${this.form.id}`
                    : '/api/audiencias/advogados';

                const res = await fetch(url, {
                    method: metodo,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        nome: this.form.nome,
                        oab: this.form.oab,
                        telefone: this.form.telefone || null,
                        email: this.form.email || null,
                        observacoes: this.form.observacoes || null
                    })
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso(this.editando ? 'Advogado atualizado!' : 'Advogado cadastrado!');
                    await this.carregar();
                    this.cancelar();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao salvar advogado:', error);
                this.mostrarErro('Erro ao salvar advogado');
            }
        },

        /**
         * Deletar
         */
        async deletar(id) {
            if (!confirm('Deseja realmente deletar este advogado?')) return;

            try {
                const res = await fetch(`/api/audiencias/advogados/${id}`, { method: 'DELETE' });
                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Advogado deletado!');
                    await this.carregar();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao deletar advogado:', error);
                this.mostrarErro('Erro ao deletar advogado');
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
