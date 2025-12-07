/**
 * Detalhes do Processo Judicial
 */

function processoDetalhesApp() {
    return {
        // Estado
        processo: null,
        participantes: [],
        audiencias: [],
        pessoas: [],
        carregando: true,
        modalParticipante: false,

        // Formulário de participante
        formParticipante: {
            pessoaId: null,
            tipoParticipacao: ''
        },

        /**
         * Inicialização
         */
        async init() {
            console.log('Inicializando detalhes do processo:', processoId);
            await this.carregarProcesso();
            await this.carregarParticipantes();
            await this.carregarPessoas();
            this.carregando = false;
        },

        /**
         * Carregar dados do processo
         */
        async carregarProcesso() {
            try {
                const res = await fetch(`/api/processos/${processoId}`);
                const data = await res.json();

                if (data.success) {
                    this.processo = data.dados;
                    this.audiencias = data.dados.audiencias || [];
                    console.log('Processo carregado:', this.processo);
                } else {
                    this.mostrarErro(data.message || 'Processo não encontrado');
                }
            } catch (error) {
                console.error('Erro ao carregar processo:', error);
                this.mostrarErro('Erro ao carregar processo');
            }
        },

        /**
         * Carregar participantes do processo
         */
        async carregarParticipantes() {
            try {
                const res = await fetch(`/api/processos/${processoId}/participantes`);
                const data = await res.json();

                if (data.success) {
                    this.participantes = data.dados || [];
                    console.log(`${this.participantes.length} participantes carregados`);
                }
            } catch (error) {
                console.error('Erro ao carregar participantes:', error);
                this.mostrarErro('Erro ao carregar participantes');
            }
        },

        /**
         * Carregar lista de pessoas disponíveis
         */
        async carregarPessoas() {
            try {
                const res = await fetch('/api/audiencias/pessoas');
                const data = await res.json();

                if (data.success) {
                    this.pessoas = data.dados || [];
                    console.log(`${this.pessoas.length} pessoas disponíveis`);
                }
            } catch (error) {
                console.error('Erro ao carregar pessoas:', error);
            }
        },

        /**
         * Abrir modal de adicionar participante
         */
        abrirModalParticipante() {
            this.modalParticipante = true;
            this.formParticipante = {
                pessoaId: null,
                tipoParticipacao: ''
            };
        },

        /**
         * Fechar modal
         */
        fecharModalParticipante() {
            this.modalParticipante = false;
        },

        /**
         * Salvar participante
         */
        async salvarParticipante() {
            try {
                const res = await fetch(`/api/processos/${processoId}/participantes`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        pessoaId: this.formParticipante.pessoaId,
                        tipoParticipacao: this.formParticipante.tipoParticipacao
                    })
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Participante adicionado com sucesso!');
                    await this.carregarParticipantes();
                    this.fecharModalParticipante();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('Erro ao adicionar participante:', error);
                this.mostrarErro('Erro ao adicionar participante');
            }
        },

        /**
         * Remover participante
         */
        async removerParticipante(participanteId) {
            if (!confirm('Deseja realmente remover este participante do processo?')) return;

            try {
                const res = await fetch(`/api/processos/${processoId}/participantes/${participanteId}`, {
                    method: 'DELETE'
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Participante removido!');
                    await this.carregarParticipantes();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('Erro ao remover participante:', error);
                this.mostrarErro('Erro ao remover participante');
            }
        },

        /**
         * Editar processo (redireciona para listagem com formulário aberto)
         */
        editarProcesso() {
            window.location.href = '/processos?editar=' + processoId;
        },

        /**
         * Formatar data completa para exibição
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
         * Formatar data simples (sem hora)
         */
        formatarDataSimples(dataStr) {
            if (!dataStr) return '-';
            // Se já estiver em formato brasileiro (dd/MM/yyyy), retornar como está
            if (dataStr.includes('/')) return dataStr;
            // Caso contrário, converter de ISO
            const data = new Date(dataStr);
            return data.toLocaleDateString('pt-BR');
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
