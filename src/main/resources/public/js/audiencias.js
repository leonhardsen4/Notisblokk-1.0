/**
 * Aplicação de Audiências Judiciais
 * Sistema de gerenciamento de audiências integrado ao Notisblokk
 */

function audienciasApp() {
    return {
        // Estado
        audiencias: [],
        audienciasProcessadas: [],
        audienciaSelecionada: null,
        participantesDetalhes: [],
        pautaDia: [],
        varas: [],
        juizes: [],
        promotores: [],
        alertas: [],
        alertasPorCriticidade: {
            CRITICO: [],
            ALTO: [],
            MEDIO: []
        },

        // Seleção múltipla
        audienciasSelecionadas: [],
        todasSelecionadas: false,

        // Modais
        modalCadastros: false,
        modalDetalhes: false,
        modalPauta: false,
        modalHorariosLivres: false,
        modalAlertas: false,
        abaAtivaCadastro: 'varas',
        abaAtivaAlertas: '3',

        // Controle de colunas
        mostrarControlesColunas: false,
        colunasVisiveis: {
            processo: true,
            data: true,
            horario: true,
            vara: true,
            tipo: true,
            status: true
        },

        // Formulários de cadastro
        formVara: { ativo: false, id: null, nome: '', comarca: '' },
        formJuiz: { ativo: false, id: null, nome: '', telefone: '', email: '' },
        formPromotor: { ativo: false, id: null, nome: '', telefone: '', email: '' },

        // Horários Livres
        horariosLivres: {
            dataInicio: '',
            dataFim: '',
            varaId: '',
            duracaoMinutos: 60,
            slots: [],
            buscaRealizada: false
        },

        // Filtros
        filtroData: '',
        filtroVara: '',
        filtroStatus: '',
        termoPesquisa: '',

        // Ordenação
        colunaOrdenacao: 'dataAudiencia',
        direcaoOrdenacao: 'asc',

        pesquisaTimeout: null,

        /**
         * Inicialização
         */
        async init() {
            console.log('DEBUG_AUDIENCIAS: Inicializando aplicação...');

            try {
                await this.carregarVaras();
                await this.carregarJuizes();
                await this.carregarPromotores();
                await this.carregarAudiencias();
                await this.carregarPautaDia();
                await this.carregarAlertas(7); // Carregar alertas dos próximos 7 dias

                console.log('DEBUG_AUDIENCIAS: Aplicação inicializada com sucesso');
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro na inicialização:', error);
                this.mostrarErro('Erro ao inicializar aplicação');
            }
        },

        /**
         * Carregar audiências
         */
        async carregarAudiencias() {
            try {
                console.log('DEBUG_AUDIENCIAS: Carregando audiências...');
                const res = await fetch('/api/audiencias');
                const data = await res.json();

                if (!data.success) {
                    throw new Error(data.message || 'Erro ao carregar audiências');
                }

                this.audiencias = data.dados || [];
                console.log(`DEBUG_AUDIENCIAS: ${this.audiencias.length} audiências carregadas`);
                this.processarAudiencias();
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar audiências:', error);
                this.mostrarErro('Erro ao carregar audiências');
            }
        },

        /**
         * Carregar varas
         */
        async carregarVaras() {
            try {
                const res = await fetch('/api/audiencias/varas');
                const data = await res.json();

                if (data.success) {
                    this.varas = data.dados || [];
                    console.log(`DEBUG_AUDIENCIAS: ${this.varas.length} varas carregadas`);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar varas:', error);
            }
        },

        /**
         * Carregar juízes
         */
        async carregarJuizes() {
            try {
                const res = await fetch('/api/audiencias/juizes');
                const data = await res.json();

                if (data.success) {
                    this.juizes = data.dados || [];
                    console.log(`DEBUG_AUDIENCIAS: ${this.juizes.length} juízes carregados`);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar juízes:', error);
            }
        },

        /**
         * Carregar promotores
         */
        async carregarPromotores() {
            try {
                const res = await fetch('/api/audiencias/promotores');
                const data = await res.json();

                if (data.success) {
                    this.promotores = data.dados || [];
                    console.log(`DEBUG_AUDIENCIAS: ${this.promotores.length} promotores carregados`);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar promotores:', error);
            }
        },

        /**
         * Carregar pauta do dia
         */
        async carregarPautaDia() {
            try {
                const res = await fetch('/api/audiencias/pauta');
                const data = await res.json();

                if (data.success) {
                    this.pautaDia = data.audiencias || [];
                    console.log(`DEBUG_AUDIENCIAS: Pauta do dia: ${this.pautaDia.length} audiências`);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar pauta:', error);
            }
        },

        /**
         * Processar e filtrar audiências
         */
        processarAudiencias() {
            console.log('DEBUG_AUDIENCIAS: Processando audiências...');

            let resultado = [...this.audiencias];

            // Filtro por data
            if (this.filtroData) {
                resultado = resultado.filter(a => {
                    // Converter dd/MM/yyyy para comparação com date input (yyyy-MM-dd)
                    const [dia, mes, ano] = a.dataAudiencia.split('/');
                    const dataAudiencia = `${ano}-${mes}-${dia}`;
                    return dataAudiencia === this.filtroData;
                });
            }

            // Filtro por vara
            if (this.filtroVara) {
                resultado = resultado.filter(a => a.vara?.id == this.filtroVara);
            }

            // Filtro por status
            if (this.filtroStatus) {
                resultado = resultado.filter(a => a.status === this.filtroStatus);
            }

            // Pesquisa textual
            if (this.termoPesquisa.trim()) {
                const termo = this.termoPesquisa.toLowerCase();
                resultado = resultado.filter(a =>
                    a.numeroProcesso?.toLowerCase().includes(termo) ||
                    a.vara?.nome?.toLowerCase().includes(termo) ||
                    a.juiz?.nome?.toLowerCase().includes(termo) ||
                    a.tipoAudiencia?.toLowerCase().includes(termo)
                );
            }

            // Ordenação
            resultado.sort((a, b) => {
                let valorA, valorB;

                switch(this.colunaOrdenacao) {
                    case 'numeroProcesso':
                        valorA = a.numeroProcesso?.toLowerCase() || '';
                        valorB = b.numeroProcesso?.toLowerCase() || '';
                        break;
                    case 'dataAudiencia':
                        valorA = this.dataParaOrdenacao(a.dataAudiencia);
                        valorB = this.dataParaOrdenacao(b.dataAudiencia);
                        break;
                    case 'horarioInicio':
                        valorA = a.horarioInicio || '';
                        valorB = b.horarioInicio || '';
                        break;
                    case 'varaNome':
                        valorA = a.vara?.nome?.toLowerCase() || '';
                        valorB = b.vara?.nome?.toLowerCase() || '';
                        break;
                    case 'tipoAudiencia':
                        valorA = a.tipoAudiencia?.toLowerCase() || '';
                        valorB = b.tipoAudiencia?.toLowerCase() || '';
                        break;
                    case 'status':
                        valorA = a.status?.toLowerCase() || '';
                        valorB = b.status?.toLowerCase() || '';
                        break;
                    default:
                        valorA = a[this.colunaOrdenacao] || '';
                        valorB = b[this.colunaOrdenacao] || '';
                }

                if (valorA < valorB) return this.direcaoOrdenacao === 'asc' ? -1 : 1;
                if (valorA > valorB) return this.direcaoOrdenacao === 'asc' ? 1 : -1;
                return 0;
            });

            this.audienciasProcessadas = resultado;
            console.log(`DEBUG_AUDIENCIAS: ${resultado.length} audiências após filtros`);
        },

        /**
         * Converter data dd/MM/yyyy para ordenação
         */
        dataParaOrdenacao(dataStr) {
            if (!dataStr) return 0;
            const [dia, mes, ano] = dataStr.split('/');
            return new Date(ano, mes - 1, dia).getTime();
        },

        /**
         * Aplicar filtros
         */
        aplicarFiltros() {
            this.processarAudiencias();
        },

        /**
         * Limpar filtros
         */
        limparFiltros() {
            this.filtroData = '';
            this.filtroVara = '';
            this.filtroStatus = '';
            this.termoPesquisa = '';
            this.processarAudiencias();
        },

        /**
         * Pesquisar com debounce
         */
        async pesquisarAudiencias() {
            clearTimeout(this.pesquisaTimeout);
            this.pesquisaTimeout = setTimeout(async () => {
                // Se houver termo de pesquisa, usar busca avançada
                if (this.termoPesquisa.trim()) {
                    try {
                        const res = await fetch(`/api/audiencias/buscar?q=${encodeURIComponent(this.termoPesquisa)}`);
                        const data = await res.json();
                        if (data.success) {
                            this.audiencias = data.dados || [];
                            this.processarAudiencias();
                        }
                    } catch (error) {
                        console.error('Erro na busca avançada:', error);
                        this.mostrarErro('Erro ao buscar audiências');
                    }
                } else {
                    // Sem termo de pesquisa, recarregar todas
                    await this.carregarAudiencias();
                }
            }, 300);
        },

        /**
         * Ordenar por coluna
         */
        ordenarPor(coluna) {
            if (this.colunaOrdenacao === coluna) {
                this.direcaoOrdenacao = this.direcaoOrdenacao === 'asc' ? 'desc' : 'asc';
            } else {
                this.colunaOrdenacao = coluna;
                this.direcaoOrdenacao = 'asc';
            }
            this.processarAudiencias();
        },

        /**
         * Ícone de ordenação
         */
        getSortIcon(coluna) {
            if (this.colunaOrdenacao !== coluna) return '⇅';
            return this.direcaoOrdenacao === 'asc' ? '↑' : '↓';
        },

        /**
         * Classe CSS da linha baseada na data
         */
        getRowClass(audiencia) {
            const hoje = new Date();
            hoje.setHours(0, 0, 0, 0);

            const [dia, mes, ano] = audiencia.dataAudiencia.split('/');
            const dataAudiencia = new Date(ano, mes - 1, dia);
            dataAudiencia.setHours(0, 0, 0, 0);

            if (dataAudiencia.getTime() === hoje.getTime()) {
                return 'audiencia-hoje';
            }

            if (dataAudiencia < hoje && audiencia.status === 'DESIGNADA') {
                return 'audiencia-atrasada';
            }

            return '';
        },

        /**
         * Visualizar detalhes da audiência
         */
        async visualizarAudiencia(audiencia) {
            this.audienciaSelecionada = audiencia;
            this.participantesDetalhes = [];
            this.modalDetalhes = true;

            // Carregar participantes da audiência
            try {
                const res = await fetch(`/api/audiencias/participacoes/audiencia/${audiencia.id}`);
                if (res.ok) {
                    const data = await res.json();
                    if (data.success && data.dados) {
                        this.participantesDetalhes = data.dados;
                        console.log('DEBUG_AUDIENCIAS: Participantes carregados:', this.participantesDetalhes.length);
                    }
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar participantes:', error);
            }
        },

        /**
         * Editar audiência
         */
        editarAudiencia(id) {
            window.location.href = `/audiencias/editar/${id}`;
        },

        /**
         * Deletar audiência
         */
        async deletarAudiencia(id) {
            if (!confirm('Deseja realmente deletar esta audiência?')) return;

            try {
                const res = await fetch(`/api/audiencias/${id}`, { method: 'DELETE' });
                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Audiência deletada com sucesso');
                    await this.carregarAudiencias();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao deletar:', error);
                this.mostrarErro('Erro ao deletar audiência');
            }
        },

        /**
         * Verificar conflitos
         */
        async verificarConflitos() {
            try {
                console.log('DEBUG_AUDIENCIAS: Verificando conflitos de horários...');

                const res = await fetch('/api/audiencias/conflitos');
                const data = await res.json();

                if (!data.success) {
                    throw new Error(data.message || 'Erro ao verificar conflitos');
                }

                const conflitos = data.dados || [];

                if (conflitos.length === 0) {
                    this.mostrarSucesso('✅ Nenhum conflito de horário detectado!');
                    console.log('DEBUG_AUDIENCIAS: Nenhum conflito encontrado');
                } else {
                    this.mostrarErro(`⚠️ ${conflitos.length} conflito(s) de horário detectado(s)!`);
                    console.log(`DEBUG_AUDIENCIAS: ${conflitos.length} conflitos encontrados`);

                    // Exibir detalhes dos conflitos no console
                    conflitos.forEach((conflito, index) => {
                        console.log(`DEBUG_AUDIENCIAS: Conflito ${index + 1}:`, conflito);
                    });

                    // Você pode implementar um modal para mostrar os conflitos detalhadamente
                    this.exibirModalConflitos(conflitos);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao verificar conflitos:', error);
                this.mostrarErro('Erro ao verificar conflitos de horários');
            }
        },

        /**
         * Exibir modal com conflitos
         */
        exibirModalConflitos(conflitos) {
            let mensagem = 'CONFLITOS DE HORÁRIO DETECTADOS:\n\n';

            conflitos.forEach((conflito, index) => {
                mensagem += `${index + 1}. ${conflito}\n`;
            });

            mensagem += '\n⚠️ Por favor, ajuste os horários das audiências conflitantes.';

            alert(mensagem);
        },

        /**
         * Abrir formulário de nova audiência
         */
        abrirFormularioAudiencia() {
            window.location.href = '/audiencias/nova';
        },

        /**
         * Modal de cadastros
         */
        abrirModalCadastros() {
            this.modalCadastros = true;
        },

        fecharModalCadastros() {
            this.modalCadastros = false;
        },

        /**
         * Modal de detalhes
         */
        fecharModalDetalhes() {
            this.modalDetalhes = false;
            this.audienciaSelecionada = null;
            this.participantesDetalhes = [];
        },

        /**
         * Modal de pauta do dia
         */
        async abrirModalPauta() {
            await this.carregarPautaDia();
            this.modalPauta = true;
        },

        fecharModalPauta() {
            this.modalPauta = false;
        },

        /**
         * ===================================
         * CRUD DE VARAS
         * ===================================
         */
        novaVara() {
            this.formVara = { ativo: true, id: null, nome: '', comarca: '' };
        },

        editarVara(vara) {
            this.formVara = {
                ativo: true,
                id: vara.id,
                nome: vara.nome,
                comarca: vara.comarca || ''
            };
        },

        cancelarVara() {
            this.formVara = { ativo: false, id: null, nome: '', comarca: '' };
        },

        async salvarVara() {
            try {
                const metodo = this.formVara.id ? 'PUT' : 'POST';
                const url = this.formVara.id
                    ? `/api/audiencias/varas/${this.formVara.id}`
                    : '/api/audiencias/varas';

                const res = await fetch(url, {
                    method: metodo,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        nome: this.formVara.nome,
                        comarca: this.formVara.comarca || null
                    })
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso(this.formVara.id ? 'Vara atualizada!' : 'Vara cadastrada!');
                    await this.carregarVaras();
                    this.cancelarVara();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao salvar vara:', error);
                this.mostrarErro('Erro ao salvar vara');
            }
        },

        async deletarVara(id) {
            if (!confirm('Deseja realmente deletar esta vara?')) return;

            try {
                const res = await fetch(`/api/audiencias/varas/${id}`, { method: 'DELETE' });
                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Vara deletada!');
                    await this.carregarVaras();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao deletar vara:', error);
                this.mostrarErro('Erro ao deletar vara');
            }
        },

        /**
         * ===================================
         * CRUD DE JUÍZES
         * ===================================
         */
        novoJuiz() {
            this.formJuiz = { ativo: true, id: null, nome: '', telefone: '', email: '' };
        },

        editarJuiz(juiz) {
            this.formJuiz = {
                ativo: true,
                id: juiz.id,
                nome: juiz.nome,
                telefone: juiz.telefone || '',
                email: juiz.email || ''
            };
        },

        cancelarJuiz() {
            this.formJuiz = { ativo: false, id: null, nome: '', telefone: '', email: '' };
        },

        async salvarJuiz() {
            try {
                const metodo = this.formJuiz.id ? 'PUT' : 'POST';
                const url = this.formJuiz.id
                    ? `/api/audiencias/juizes/${this.formJuiz.id}`
                    : '/api/audiencias/juizes';

                const res = await fetch(url, {
                    method: metodo,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        nome: this.formJuiz.nome,
                        telefone: this.formJuiz.telefone || null,
                        email: this.formJuiz.email || null
                    })
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso(this.formJuiz.id ? 'Juiz atualizado!' : 'Juiz cadastrado!');
                    await this.carregarJuizes();
                    this.cancelarJuiz();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao salvar juiz:', error);
                this.mostrarErro('Erro ao salvar juiz');
            }
        },

        async deletarJuiz(id) {
            if (!confirm('Deseja realmente deletar este juiz?')) return;

            try {
                const res = await fetch(`/api/audiencias/juizes/${id}`, { method: 'DELETE' });
                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Juiz deletado!');
                    await this.carregarJuizes();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao deletar juiz:', error);
                this.mostrarErro('Erro ao deletar juiz');
            }
        },

        /**
         * ===================================
         * CRUD DE PROMOTORES
         * ===================================
         */
        novoPromotor() {
            this.formPromotor = { ativo: true, id: null, nome: '', telefone: '', email: '' };
        },

        editarPromotor(promotor) {
            this.formPromotor = {
                ativo: true,
                id: promotor.id,
                nome: promotor.nome,
                telefone: promotor.telefone || '',
                email: promotor.email || ''
            };
        },

        cancelarPromotor() {
            this.formPromotor = { ativo: false, id: null, nome: '', telefone: '', email: '' };
        },

        async salvarPromotor() {
            try {
                const metodo = this.formPromotor.id ? 'PUT' : 'POST';
                const url = this.formPromotor.id
                    ? `/api/audiencias/promotores/${this.formPromotor.id}`
                    : '/api/audiencias/promotores';

                const res = await fetch(url, {
                    method: metodo,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        nome: this.formPromotor.nome,
                        telefone: this.formPromotor.telefone || null,
                        email: this.formPromotor.email || null
                    })
                });

                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso(this.formPromotor.id ? 'Promotor atualizado!' : 'Promotor cadastrado!');
                    await this.carregarPromotores();
                    this.cancelarPromotor();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao salvar promotor:', error);
                this.mostrarErro('Erro ao salvar promotor');
            }
        },

        async deletarPromotor(id) {
            if (!confirm('Deseja realmente deletar este promotor?')) return;

            try {
                const res = await fetch(`/api/audiencias/promotores/${id}`, { method: 'DELETE' });
                const data = await res.json();

                if (data.success) {
                    this.mostrarSucesso('Promotor deletado!');
                    await this.carregarPromotores();
                } else {
                    this.mostrarErro(data.message);
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao deletar promotor:', error);
                this.mostrarErro('Erro ao deletar promotor');
            }
        },

        /**
         * Notificações (usando Toastify.js)
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
        },

        mostrarInfo(mensagem) {
            if (typeof Toastify !== 'undefined') {
                Toastify({
                    text: mensagem,
                    duration: 3000,
                    close: true,
                    gravity: "top",
                    position: "right",
                    backgroundColor: "#3B82F6",
                }).showToast();
            } else {
                alert(mensagem);
            }
        },

        // ====================================================================
        // HORÁRIOS LIVRES
        // ====================================================================

        /**
         * Abre modal de busca de horários livres
         */
        abrirModalHorariosLivres() {
            // Definir datas padrão (próximos 7 dias)
            const hoje = new Date();
            const proxSemana = new Date();
            proxSemana.setDate(hoje.getDate() + 7);

            this.horariosLivres.dataInicio = hoje.toISOString().split('T')[0];
            this.horariosLivres.dataFim = proxSemana.toISOString().split('T')[0];
            this.horariosLivres.varaId = '';
            this.horariosLivres.duracaoMinutos = 60;
            this.horariosLivres.slots = [];
            this.horariosLivres.buscaRealizada = false;

            this.modalHorariosLivres = true;
            console.log('DEBUG_AUDIENCIAS: Modal de horários livres aberto');
        },

        /**
         * Fecha modal de horários livres
         */
        fecharModalHorariosLivres() {
            this.modalHorariosLivres = false;
        },

        /**
         * Busca horários livres na API
         */
        async buscarHorariosLivres() {
            try {
                // Validações
                if (!this.horariosLivres.dataInicio || !this.horariosLivres.dataFim) {
                    this.mostrarErro('Informe o período de busca');
                    return;
                }

                if (this.horariosLivres.dataInicio > this.horariosLivres.dataFim) {
                    this.mostrarErro('Data inicial deve ser anterior à data final');
                    return;
                }

                console.log('DEBUG_AUDIENCIAS: Buscando horários livres...');

                // Converter datas para formato dd/MM/yyyy
                const dataInicio = this.converterDataParaBR(this.horariosLivres.dataInicio);
                const dataFim = this.converterDataParaBR(this.horariosLivres.dataFim);

                // Montar payload - sempre sem espaçamento (grade = 0, buffer = 0)
                const payload = {
                    dataInicio: dataInicio,
                    dataFim: dataFim,
                    varaId: this.horariosLivres.varaId || null,
                    duracaoMinutos: parseInt(this.horariosLivres.duracaoMinutos),
                    bufferAntesMinutos: 0,
                    bufferDepoisMinutos: 0,
                    gradeMinutos: 0,
                    gapMinimoMinutos: 0
                };

                console.log('DEBUG_AUDIENCIAS: Payload:', payload);

                // Fazer requisição
                const response = await fetch('/api/audiencias/horarios-livres', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                const data = await response.json();

                if (data.success) {
                    this.horariosLivres.slots = data.dados || [];
                    this.horariosLivres.buscaRealizada = true;

                    console.log(`DEBUG_AUDIENCIAS: ${this.horariosLivres.slots.length} horários encontrados`);

                    this.mostrarInfo(`${this.horariosLivres.slots.length} horários disponíveis encontrados`);
                } else {
                    this.mostrarErro(data.message || 'Erro ao buscar horários livres');
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao buscar horários livres:', error);
                this.mostrarErro('Erro ao buscar horários livres');
            }
        },

        /**
         * Agrupa slots por data
         */
        agruparSlotsPorDia(slots) {
            const grupos = {};

            slots.forEach(slot => {
                const data = slot.data;
                if (!grupos[data]) {
                    grupos[data] = [];
                }
                grupos[data].push(slot);
            });

            return grupos;
        },

        /**
         * Formata data para exibição extensa
         */
        formatarDataExtenso(data) {
            // data está no formato yyyy-MM-dd ou dd/MM/yyyy
            let partes;
            if (data.includes('-')) {
                partes = data.split('-');
                // yyyy-MM-dd -> dd/MM/yyyy
                data = `${partes[2]}/${partes[1]}/${partes[0]}`;
            }

            partes = data.split('/');
            const dia = parseInt(partes[0]);
            const mes = parseInt(partes[1]) - 1;
            const ano = parseInt(partes[2]);

            const dataObj = new Date(ano, mes, dia);

            const diasSemana = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];
            const meses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

            return `${diasSemana[dataObj.getDay()]}, ${dia} de ${meses[mes]} de ${ano}`;
        },

        /**
         * Seleciona um horário livre e redireciona para formulário de nova audiência
         */
        selecionarHorario(slot) {
            console.log('DEBUG_AUDIENCIAS: Horário selecionado:', slot);

            // Converter data e horário para formato do formulário
            let data = slot.data;
            if (data.includes('/')) {
                // dd/MM/yyyy -> yyyy-MM-dd
                const partes = data.split('/');
                data = `${partes[2]}-${partes[1]}-${partes[0]}`;
            }

            const horario = slot.horarioInicio.substring(0, 5); // HH:mm

            // Armazenar no sessionStorage para popular o formulário
            sessionStorage.setItem('novaAudienciaData', data);
            sessionStorage.setItem('novaAudienciaHorario', horario);
            sessionStorage.setItem('novaAudienciaDuracao', slot.duracaoMinutos);
            if (this.horariosLivres.varaId) {
                sessionStorage.setItem('novaAudienciaVara', this.horariosLivres.varaId);
            }

            this.mostrarSucesso('Horário selecionado! Redirecionando...');

            // Redirecionar para formulário de nova audiência
            setTimeout(() => {
                window.location.href = '/audiencias/nova';
            }, 500);
        },

        /**
         * Converte data de yyyy-MM-dd para dd/MM/yyyy
         */
        converterDataParaBR(dataISO) {
            if (!dataISO) return '';
            const partes = dataISO.split('-');
            return `${partes[2]}/${partes[1]}/${partes[0]}`;
        },

        // ====================================================================
        // IMPRESSÃO DE PAUTA
        // ====================================================================

        /**
         * Imprime a pauta do dia em formato PDF
         */
        async imprimirPauta() {
            if (this.pautaDia.length === 0) {
                this.mostrarErro('Nenhuma audiência para imprimir');
                return;
            }

            console.log('DEBUG_AUDIENCIAS: Preparando impressão da pauta com detalhes...');

            try {
                // Carregar participantes de todas as audiências da pauta
                const audienciasComDetalhes = await Promise.all(
                    this.pautaDia.map(async (audiencia) => {
                        try {
                            const res = await fetch(`/api/audiencias/participacoes/audiencia/${audiencia.id}`);
                            const data = await res.json();
                            return {
                                ...audiencia,
                                participantes: data.success ? (data.dados || []) : []
                            };
                        } catch (error) {
                            console.error('Erro ao carregar participantes da audiência', audiencia.id, error);
                            return { ...audiencia, participantes: [] };
                        }
                    })
                );

                // Criar janela de impressão
                const janelaImpressao = window.open('', '_blank', 'width=800,height=600');

                if (!janelaImpressao) {
                    this.mostrarErro('Erro ao abrir janela de impressão. Verifique se pop-ups estão bloqueados.');
                    return;
                }

                // Data de hoje formatada
                const hoje = new Date();
                const dataFormatada = hoje.toLocaleDateString('pt-BR', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                });

                // Montar HTML da pauta compacta
                let html = `
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Pauta do Dia - ${dataFormatada}</title>
    <style>
        @page {
            margin: 12mm;
            size: A4;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Arial', 'Helvetica', sans-serif;
            background: white;
            color: #1a1a1a;
            line-height: 1.3;
            font-size: 9pt;
        }

        .header {
            text-align: center;
            margin-bottom: 12px;
            padding-bottom: 8px;
            border-bottom: 3px solid #8B1538;
            page-break-after: avoid;
        }

        .logo-container {
            margin-bottom: 6px;
        }

        .logo {
            height: 45px;
        }

        .header-title {
            font-size: 11pt;
            font-weight: bold;
            color: #8B1538;
            margin: 4px 0 2px 0;
            text-transform: uppercase;
        }

        .header-subtitle {
            font-size: 9pt;
            color: #1a1a1a;
            font-weight: 600;
            margin: 2px 0;
        }

        .header-info {
            font-size: 7.5pt;
            color: #555;
            line-height: 1.4;
        }

        .data-pauta {
            font-size: 9pt;
            color: #333;
            font-weight: 600;
            margin: 4px 0;
            text-transform: capitalize;
        }

        .total-audiencias {
            font-size: 8pt;
            color: #666;
        }

        .audiencia-card {
            margin-bottom: 10px;
            border: 1.5px solid #8B1538;
            border-radius: 4px;
            page-break-inside: avoid;
            background: white;
        }

        .audiencia-header {
            background: linear-gradient(to bottom, #8B1538, #A52045);
            color: white;
            padding: 4px 8px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-radius: 3px 3px 0 0;
        }

        .audiencia-horario {
            font-size: 11pt;
            font-weight: bold;
        }

        .audiencia-status {
            font-size: 7.5pt;
            padding: 2px 6px;
            border-radius: 3px;
            font-weight: 600;
            background: white;
        }

        .status-designada { color: #1e40af; }
        .status-realizada { color: #065f46; }
        .status-cancelada { color: #991b1b; }
        .status-redesignada { color: #92400e; }

        .audiencia-body {
            padding: 6px 8px;
        }

        .info-row {
            display: flex;
            margin-bottom: 3px;
            font-size: 8.5pt;
        }

        .info-label {
            font-weight: 600;
            color: #8B1538;
            min-width: 75px;
            flex-shrink: 0;
        }

        .info-value {
            color: #1a1a1a;
        }

        .participantes-section {
            margin-top: 5px;
            padding-top: 5px;
            border-top: 1px dashed #ccc;
        }

        .participantes-title {
            font-size: 8pt;
            font-weight: 600;
            color: #8B1538;
            margin-bottom: 3px;
        }

        .participante-item {
            margin-bottom: 2px;
            padding: 3px 5px;
            background: #f8f9fa;
            border-left: 2px solid #8B1538;
            font-size: 7.5pt;
            line-height: 1.3;
        }

        .participante-nome {
            font-weight: 600;
            color: #1a1a1a;
        }

        .participante-tipo {
            color: #555;
            font-size: 7pt;
            text-transform: uppercase;
        }

        .participante-advogado {
            color: #8B1538;
            font-style: italic;
            margin-top: 1px;
        }

        .observacoes-box {
            margin-top: 4px;
            padding: 4px 6px;
            background: #fffbeb;
            border-left: 3px solid #f59e0b;
            font-size: 7.5pt;
            font-style: italic;
            color: #78350f;
        }

        .rodape {
            margin-top: 15px;
            text-align: center;
            font-size: 6.5pt;
            color: #888;
            border-top: 1px solid #ddd;
            padding-top: 6px;
        }

        @media print {
            body {
                padding: 0;
            }

            .audiencia-card {
                page-break-inside: avoid;
            }

            .header {
                page-break-after: avoid;
            }
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="logo-container">
            <img src="https://www.tjsp.jus.br/download/portal/memoria/Logotipo_TJSP_Fundo_Branco_WEB.jpg" alt="TJSP" class="logo" onerror="this.style.display='none'">
        </div>
        <div class="header-title">TRIBUNAL DE JUSTIÇA DE SÃO PAULO</div>
        <div class="header-subtitle">Comarca de Cotia - SP</div>
        <div class="header-info">
            Rua Topázio, 585, Jardim Nomura, CEP 06717-235, Cotia - SP
        </div>
        <div class="data-pauta">${dataFormatada}</div>
        <div class="total-audiencias">Total de Audiências: ${audienciasComDetalhes.length}</div>
    </div>
`;

                // Adicionar cada audiência como card compacto
                audienciasComDetalhes.forEach(audiencia => {
                    const statusClass = `status-${audiencia.status.toLowerCase()}`;

                    html += `
    <div class="audiencia-card">
        <div class="audiencia-header">
            <span class="audiencia-horario">🕐 ${audiencia.horarioInicio} - ${audiencia.horarioFim}</span>
            <span class="audiencia-status ${statusClass}">${audiencia.status}</span>
        </div>
        <div class="audiencia-body">
            <div class="info-row">
                <span class="info-label">Processo:</span>
                <span class="info-value"><strong>${audiencia.numeroProcesso}</strong></span>
            </div>
            <div class="info-row">
                <span class="info-label">Vara:</span>
                <span class="info-value">${audiencia.vara?.nome || 'N/A'} - ${audiencia.competencia || 'N/A'}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Tipo/Formato:</span>
                <span class="info-value">${audiencia.tipoAudiencia} - ${audiencia.formato}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Juiz:</span>
                <span class="info-value">${audiencia.juiz?.nome || 'Não informado'}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Promotor:</span>
                <span class="info-value">${audiencia.promotor?.nome || 'Não informado'}</span>
            </div>
            ${audiencia.artigo ? `
            <div class="info-row">
                <span class="info-label">Artigo:</span>
                <span class="info-value">${audiencia.artigo}</span>
            </div>` : ''}
            ${audiencia.reuPreso || audiencia.agendamentoTeams || audiencia.reconhecimento || audiencia.depoimentoEspecial ? `
            <div class="info-row">
                <span class="info-label">Observações:</span>
                <span class="info-value">
                    ${audiencia.reuPreso ? '⚠️ Réu Preso ' : ''}
                    ${audiencia.agendamentoTeams ? '💻 Teams ' : ''}
                    ${audiencia.reconhecimento ? '👁️ Reconhecimento ' : ''}
                    ${audiencia.depoimentoEspecial ? '🎤 Dep. Especial' : ''}
                </span>
            </div>` : ''}

            ${audiencia.participantes && audiencia.participantes.length > 0 ? `
            <div class="participantes-section">
                <div class="participantes-title">👥 Participantes (${audiencia.participantes.length}):</div>
                ${audiencia.participantes.map(p => `
                <div class="participante-item">
                    <span class="participante-nome">${p.pessoaNome}</span>
                    <span class="participante-tipo"> - ${p.tipoParticipacao}${p.intimado ? ' ✓ Intimado' : ''}</span>
                    ${p.advogadoNome ? `<div class="participante-advogado">👨‍⚖️ Advogado: ${p.advogadoNome} (OAB: ${p.advogadoOab}) - ${p.tipoRepresentacao}</div>` : ''}
                </div>`).join('')}
            </div>` : ''}

            ${audiencia.observacoes ? `
            <div class="observacoes-box">
                📝 <strong>Observações:</strong> ${audiencia.observacoes}
            </div>` : ''}
        </div>
    </div>
`;
                });

                html += `
    <div class="rodape">
        Documento gerado automaticamente pelo Sistema Notisblokk em ${new Date().toLocaleString('pt-BR')}<br>
        Este documento possui validade informativa para fins de organização da pauta de audiências
    </div>

    <script>
        window.onload = function() {
            window.print();
        };

        window.onafterprint = function() {
            window.close();
        };
    </script>
</body>
</html>`;

                // Escrever HTML na janela
                janelaImpressao.document.write(html);
                janelaImpressao.document.close();

                console.log('DEBUG_AUDIENCIAS: Pauta detalhada pronta para impressão');
                this.mostrarSucesso('Preparando impressão da pauta...');

            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao preparar pauta:', error);
                this.mostrarErro('Erro ao preparar pauta: ' + error.message);
            }
        },

        /**
         * Imprimir audiência individual em PDF
         */
        async imprimirAudiencia(audienciaId) {
            console.log('DEBUG_AUDIENCIAS: imprimirAudiencia() - ID:', audienciaId);

            if (!audienciaId) {
                this.mostrarErro('ID da audiência não informado');
                return;
            }

            try {
                // Carregar dados completos da audiência
                const resAudiencia = await fetch(`/api/audiencias/${audienciaId}`);
                const dataAudiencia = await resAudiencia.json();

                if (!dataAudiencia.success || !dataAudiencia.dados) {
                    throw new Error('Erro ao carregar dados da audiência');
                }

                const audiencia = dataAudiencia.dados;

                // Carregar participantes com dados completos
                const resParticipantes = await fetch(`/api/audiencias/participacoes/audiencia/${audienciaId}`);
                const dataParticipantes = await resParticipantes.json();

                const participantes = dataParticipantes.success ? (dataParticipantes.dados || []) : [];

                console.log('DEBUG_AUDIENCIAS: Dados carregados para impressão - Participantes:', participantes.length);

                // Criar janela de impressão
                const janelaImpressao = window.open('', '_blank', 'width=800,height=600');

                if (!janelaImpressao) {
                    throw new Error('Não foi possível abrir a janela de impressão. Verifique se o bloqueador de pop-ups está desativado.');
                }

                // Gerar HTML para impressão
                const html = `
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Audiência ${audiencia.numeroProcesso}</title>
    <style>
        @page {
            margin: 15mm;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.5;
            color: #1f2937;
            background: white;
            padding: 0;
        }

        .container {
            max-width: 210mm;
            margin: 0 auto;
            background: white;
        }

        .header {
            text-align: center;
            padding: 20px 0;
            border-bottom: 3px solid #3b82f6;
            margin-bottom: 20px;
        }

        .header h1 {
            font-size: 22px;
            color: #1f2937;
            margin-bottom: 5px;
            font-weight: 700;
        }

        .header .subtitulo {
            font-size: 14px;
            color: #6b7280;
            font-weight: 500;
        }

        .secao {
            margin-bottom: 18px;
            padding: 15px;
            background: #f9fafb;
            border-radius: 6px;
            border-left: 4px solid #3b82f6;
        }

        .secao-titulo {
            font-size: 15px;
            font-weight: 700;
            color: #1f2937;
            margin-bottom: 10px;
            padding-bottom: 6px;
            border-bottom: 2px solid #e5e7eb;
        }

        .campo {
            display: flex;
            padding: 6px 0;
            border-bottom: 1px dashed #e5e7eb;
        }

        .campo:last-child {
            border-bottom: none;
        }

        .campo-label {
            font-weight: 600;
            color: #4b5563;
            width: 140px;
            flex-shrink: 0;
        }

        .campo-valor {
            color: #1f2937;
            flex: 1;
        }

        .badge {
            display: inline-block;
            padding: 3px 10px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
        }

        .badge-designada { background: #dbeafe; color: #1e40af; }
        .badge-realizada { background: #d1fae5; color: #065f46; }
        .badge-cancelada { background: #fee2e2; color: #991b1b; }
        .badge-adiada { background: #fef3c7; color: #92400e; }

        .badge-sm {
            padding: 2px 8px;
            font-size: 10px;
        }

        .badge-success { background: #d1fae5; color: #065f46; }
        .badge-danger { background: #fee2e2; color: #991b1b; }
        .badge-info { background: #dbeafe; color: #1e40af; }
        .badge-warning { background: #fef3c7; color: #92400e; }

        .participante {
            padding: 10px;
            margin-bottom: 8px;
            background: white;
            border-radius: 4px;
            border-left: 3px solid #6366f1;
        }

        .participante-header {
            font-weight: 700;
            margin-bottom: 4px;
            color: #1f2937;
        }

        .participante-info {
            font-size: 12px;
            color: #4b5563;
            margin-top: 4px;
            padding-left: 15px;
        }

        .observacoes {
            padding: 12px;
            background: #fffbeb;
            border-left: 4px solid #f59e0b;
            border-radius: 4px;
            font-size: 13px;
            line-height: 1.6;
            color: #78350f;
        }

        .rodape {
            margin-top: 30px;
            padding-top: 15px;
            border-top: 2px solid #e5e7eb;
            text-align: center;
            font-size: 11px;
            color: #9ca3af;
        }

        @media print {
            body {
                padding: 0;
            }

            .container {
                max-width: 100%;
            }

            .secao {
                page-break-inside: avoid;
            }

            .participante {
                page-break-inside: avoid;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <!-- Cabeçalho -->
        <div class="header">
            <h1>⚖️ AUDIÊNCIA JUDICIAL</h1>
            <div class="subtitulo">Processo nº ${audiencia.numeroProcesso}</div>
        </div>

        <!-- Informações Básicas -->
        <div class="secao">
            <div class="secao-titulo">📋 Informações Básicas</div>
            <div class="campo">
                <span class="campo-label">Número do Processo:</span>
                <span class="campo-valor"><strong>${audiencia.numeroProcesso}</strong></span>
            </div>
            <div class="campo">
                <span class="campo-label">Data:</span>
                <span class="campo-valor">${audiencia.dataAudiencia} (${audiencia.diaSemana})</span>
            </div>
            <div class="campo">
                <span class="campo-label">Horário:</span>
                <span class="campo-valor">${audiencia.horarioInicio} - ${audiencia.horarioFim} (${audiencia.duracao} minutos)</span>
            </div>
            <div class="campo">
                <span class="campo-label">Vara:</span>
                <span class="campo-valor">${audiencia.vara?.nome || 'N/A'}</span>
            </div>
            <div class="campo">
                <span class="campo-label">Competência:</span>
                <span class="campo-valor">${audiencia.competencia || 'N/A'}</span>
            </div>
            <div class="campo">
                <span class="campo-label">Tipo:</span>
                <span class="campo-valor">${audiencia.tipoAudiencia || 'N/A'}</span>
            </div>
            <div class="campo">
                <span class="campo-label">Formato:</span>
                <span class="campo-valor">${audiencia.formato || 'N/A'}</span>
            </div>
            <div class="campo">
                <span class="campo-label">Status:</span>
                <span class="campo-valor"><span class="badge badge-${audiencia.status.toLowerCase()}">${audiencia.status}</span></span>
            </div>
        </div>

        <!-- Magistrado e Promotor -->
        <div class="secao">
            <div class="secao-titulo">👨‍⚖️ Magistrado e Promotoria</div>
            <div class="campo">
                <span class="campo-label">Juiz:</span>
                <span class="campo-valor">${audiencia.juiz?.nome || 'Não informado'}</span>
            </div>
            <div class="campo">
                <span class="campo-label">Promotor:</span>
                <span class="campo-valor">${audiencia.promotor?.nome || 'Não informado'}</span>
            </div>
        </div>

        ${audiencia.artigo || audiencia.reuPreso || audiencia.agendamentoTeams || audiencia.reconhecimento || audiencia.depoimentoEspecial ? `
        <!-- Dados Adicionais -->
        <div class="secao">
            <div class="secao-titulo">⚖️ Informações Adicionais</div>
            ${audiencia.artigo ? `
            <div class="campo">
                <span class="campo-label">Artigo:</span>
                <span class="campo-valor">${audiencia.artigo}</span>
            </div>` : ''}
            <div class="campo">
                <span class="campo-label">Situação Especial:</span>
                <span class="campo-valor">
                    ${audiencia.reuPreso ? '<span class="badge badge-sm badge-danger">Réu Preso</span> ' : ''}
                    ${audiencia.agendamentoTeams ? '<span class="badge badge-sm badge-info">Agend. Teams</span> ' : ''}
                    ${audiencia.reconhecimento ? '<span class="badge badge-sm badge-warning">Reconhecimento</span> ' : ''}
                    ${audiencia.depoimentoEspecial ? '<span class="badge badge-sm badge-warning">Dep. Especial</span> ' : ''}
                </span>
            </div>
        </div>` : ''}

        ${participantes.length > 0 ? `
        <!-- Participantes -->
        <div class="secao">
            <div class="secao-titulo">👥 Participantes do Processo (${participantes.length})</div>
            ${participantes.map(p => `
            <div class="participante">
                <div class="participante-header">
                    ${p.pessoaNome}
                    <span class="badge badge-sm">${p.tipoParticipacao}</span>
                    ${p.intimado ? '<span class="badge badge-sm badge-success">✓ Intimado</span>' : ''}
                </div>
                ${p.advogadoNome ? `
                <div class="participante-info">
                    👨‍⚖️ Advogado: <strong>${p.advogadoNome}</strong> (OAB: ${p.advogadoOab}) - ${p.tipoRepresentacao}
                </div>` : ''}
                ${p.observacoes ? `
                <div class="participante-info">
                    <em>Obs: ${p.observacoes}</em>
                </div>` : ''}
            </div>`).join('')}
        </div>` : ''}

        ${audiencia.observacoes ? `
        <!-- Observações -->
        <div class="secao">
            <div class="secao-titulo">📝 Observações da Audiência</div>
            <div class="observacoes">${audiencia.observacoes}</div>
        </div>` : ''}

        <!-- Rodapé -->
        <div class="rodape">
            Documento gerado automaticamente pelo sistema Notisblokk em ${new Date().toLocaleString('pt-BR')}<br>
            Este documento possui validade informativa para fins de organização interna
        </div>
    </div>

    <script>
        // Auto-imprimir quando carregar
        window.onload = function() {
            window.print();
        };

        // Fechar após imprimir ou cancelar
        window.onafterprint = function() {
            window.close();
        };
    </script>
</body>
</html>`;

                // Escrever HTML na janela
                janelaImpressao.document.write(html);
                janelaImpressao.document.close();

                console.log('DEBUG_AUDIENCIAS: Janela de impressão da audiência aberta');
                this.mostrarSucesso('Preparando impressão da audiência...');

            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao imprimir audiência:', error);
                this.mostrarErro('Erro ao gerar PDF: ' + error.message);
            }
        },

        /**
         * Carregar alertas de audiências com pendências
         */
        async carregarAlertas(dias) {
            try {
                const res = await fetch(`/api/audiencias/alertas/proximas?dias=${dias}`);
                const data = await res.json();

                if (data.success) {
                    this.alertas = data.dados || [];

                    // Agrupar por criticidade
                    this.alertasPorCriticidade = {
                        CRITICO: this.alertas.filter(a => a.nivelCriticidade === 'CRITICO'),
                        ALTO: this.alertas.filter(a => a.nivelCriticidade === 'ALTO'),
                        MEDIO: this.alertas.filter(a => a.nivelCriticidade === 'MEDIO')
                    };

                    console.log(`Alertas carregados: ${this.alertas.length} total`);
                }
            } catch (error) {
                console.error('Erro ao carregar alertas:', error);
            }
        },

        /**
         * Alternar seleção de audiência
         */
        toggleSelecaoAudiencia(audienciaId) {
            const index = this.audienciasSelecionadas.indexOf(audienciaId);
            if (index > -1) {
                this.audienciasSelecionadas.splice(index, 1);
            } else {
                this.audienciasSelecionadas.push(audienciaId);
            }
            this.atualizarSelecaoTodas();
        },

        /**
         * Selecionar/Deselecionar todas
         */
        toggleSelecionarTodas() {
            if (this.todasSelecionadas) {
                this.audienciasSelecionadas = [];
                this.todasSelecionadas = false;
            } else {
                this.audienciasSelecionadas = this.audienciasProcessadas.map(a => a.id);
                this.todasSelecionadas = true;
            }
        },

        /**
         * Atualizar estado de "todas selecionadas"
         */
        atualizarSelecaoTodas() {
            this.todasSelecionadas = this.audienciasProcessadas.length > 0 &&
                                      this.audienciasSelecionadas.length === this.audienciasProcessadas.length;
        },

        /**
         * Limpar seleção
         */
        limparSelecao() {
            this.audienciasSelecionadas = [];
            this.todasSelecionadas = false;
        },

        /**
         * Deletar múltiplas audiências
         */
        async deletarSelecionadas() {
            if (this.audienciasSelecionadas.length === 0) {
                this.mostrarErro('Nenhuma audiência selecionada');
                return;
            }

            const confirmacao = confirm(`Deseja realmente deletar ${this.audienciasSelecionadas.length} audiência(s) selecionada(s)?`);
            if (!confirmacao) return;

            try {
                const res = await fetch('/api/audiencias/deletar-multiplas', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ ids: this.audienciasSelecionadas })
                });

                const data = await res.json();

                if (data.success || data.deletados > 0) {
                    this.mostrarSucesso(`${data.deletados} audiência(s) deletada(s) com sucesso`);
                    if (data.erros > 0) {
                        this.mostrarErro(`${data.erros} erro(s) ao deletar`);
                    }
                    await this.carregarAudiencias();
                    this.limparSelecao();
                } else {
                    this.mostrarErro(data.message || 'Erro ao deletar audiências');
                }
            } catch (error) {
                console.error('Erro ao deletar audiências:', error);
                this.mostrarErro('Erro ao deletar audiências');
            }
        },

        /**
         * Mudar status de múltiplas audiências
         */
        async mudarStatusSelecionadas(novoStatus) {
            if (this.audienciasSelecionadas.length === 0) {
                this.mostrarErro('Nenhuma audiência selecionada');
                return;
            }

            try {
                const res = await fetch('/api/audiencias/mudar-status', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        ids: this.audienciasSelecionadas,
                        novoStatus: novoStatus
                    })
                });

                const data = await res.json();

                if (data.success || data.atualizados > 0) {
                    this.mostrarSucesso(`${data.atualizados} audiência(s) atualizada(s) para ${novoStatus}`);
                    if (data.erros > 0) {
                        this.mostrarErro(`${data.erros} erro(s) ao atualizar`);
                    }
                    await this.carregarAudiencias();
                    this.limparSelecao();
                } else {
                    this.mostrarErro(data.message || 'Erro ao mudar status');
                }
            } catch (error) {
                console.error('Erro ao mudar status:', error);
                this.mostrarErro('Erro ao mudar status');
            }
        },

        /**
         * Abrir modal de alertas
         */
        abrirModalAlertas() {
            this.modalAlertas = true;
        },

        /**
         * Fechar modal de alertas
         */
        fecharModalAlertas() {
            this.modalAlertas = false;
        },

        /**
         * Atualizar alertas ao trocar de aba
         */
        async trocarAbaAlertas(dias) {
            this.abaAtivaAlertas = dias.toString();
            await this.carregarAlertas(dias);
        },

        /**
         * Ir para audiência a partir do alerta
         */
        irParaAudiencia(audienciaId) {
            this.fecharModalAlertas();
            const audiencia = this.audiencias.find(a => a.id === audienciaId);
            if (audiencia) {
                this.visualizarAudiencia(audiencia);
            }
        }
    }
}
