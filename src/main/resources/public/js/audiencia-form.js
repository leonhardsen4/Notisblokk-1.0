/**
 * Formulário de Audiências
 * Controla criação e edição de audiências
 */

function audienciaFormApp() {
    return {
        // Estado
        audienciaId: null,
        salvando: false,
        varas: [],
        juizes: [],
        promotores: [],
        pessoas: [],
        advogados: [],
        verificandoConflito: false,
        conflitosEncontrados: [],
        temConflito: false,
        timeoutConflito: null,

        // Autocomplete para pessoas e advogados
        buscaPessoa: '',
        buscaAdvogado: '',
        mostrarListaPessoas: false,
        mostrarListaAdvogados: false,
        mostrarBotaoNovaPessoa: false,
        mostrarBotaoNovoAdvogado: false,

        // Formulário
        form: {
            numeroProcesso: '',
            varaId: '',
            dataAudiencia: '',
            horarioInicio: '',
            horarioFim: '',
            tipoAudiencia: '',
            formato: '',
            status: 'DESIGNADA',
            juizId: '',
            promotorId: '',
            artigo: '',
            reuPreso: false,
            agendamentoTeams: false,
            reconhecimento: false,
            depoimentoEspecial: false,
            observacoes: '',
            participantes: [],
            representacoes: []
        },

        // Formulários auxiliares
        novoParticipante: {
            pessoaId: '',
            pessoaNome: '',
            tipoParticipacao: '',
            intimado: false,
            observacoes: '',
            // Dados do advogado (incluídos junto com o participante)
            advogadoId: '',
            advogadoNome: '',
            advogadoOab: '',
            tipoRepresentacao: ''
        },

        novaRepresentacao: {
            advogadoId: '',
            clienteId: '',
            tipoRepresentacao: ''
        },

        // ====================================================================
        // COMPUTED PROPERTIES (GETTERS)
        // ====================================================================

        /**
         * Filtra pessoas com base na busca
         */
        get pessoasFiltradas() {
            if (!this.buscaPessoa || this.buscaPessoa.trim() === '') {
                return this.pessoas;
            }

            const termo = this.buscaPessoa.toLowerCase();
            const filtradas = this.pessoas.filter(p =>
                p.nome.toLowerCase().includes(termo) ||
                (p.cpf && p.cpf.includes(termo)) ||
                (p.telefone && p.telefone.includes(termo)) ||
                (p.email && p.email.toLowerCase().includes(termo))
            );

            // Mostrar botão de adicionar se não houver resultados
            this.mostrarBotaoNovaPessoa = filtradas.length === 0 && this.buscaPessoa.trim().length >= 3;

            return filtradas;
        },

        /**
         * Filtra advogados com base na busca
         */
        get advogadosFiltrados() {
            if (!this.buscaAdvogado || this.buscaAdvogado.trim() === '') {
                return this.advogados;
            }

            const termo = this.buscaAdvogado.toLowerCase();
            const filtrados = this.advogados.filter(a =>
                a.nome.toLowerCase().includes(termo) ||
                (a.oab && a.oab.toLowerCase().includes(termo)) ||
                (a.telefone && a.telefone.includes(termo)) ||
                (a.email && a.email.toLowerCase().includes(termo))
            );

            // Mostrar botão de adicionar se não houver resultados
            this.mostrarBotaoNovoAdvogado = filtrados.length === 0 && this.buscaAdvogado.trim().length >= 3;

            return filtrados;
        },

        /**
         * Retorna apenas pessoas que já foram adicionadas como participantes
         */
        get pessoasParticipantes() {
            return this.form.participantes.map(p => {
                const pessoa = this.pessoas.find(pes => pes.id == p.pessoaId);
                return pessoa;
            }).filter(p => p !== undefined);
        },

        /**
         * Inicialização
         */
        async init() {
            console.log('DEBUG_AUDIENCIAS: Inicializando formulário...');

            // Extrair ID da URL se estiver editando
            const pathParts = window.location.pathname.split('/');
            if (pathParts.includes('editar')) {
                this.audienciaId = pathParts[pathParts.length - 1];
                console.log('DEBUG_AUDIENCIAS: Modo edição - ID:', this.audienciaId);
            }

            try {
                await this.carregarDadosAuxiliares();

                if (this.audienciaId) {
                    await this.carregarAudiencia();
                } else {
                    // Se não está editando, verificar se há dados de horário selecionado
                    this.preencherDadosHorarioSelecionado();
                }

                // Iniciar observação de conflitos
                this.observarCamposConflito();

                // Adicionar listener para fechar autocomplete ao clicar fora
                this.configurarFechamentoAutocomplete();

                console.log('DEBUG_AUDIENCIAS: Formulário inicializado');
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao inicializar:', error);
                this.mostrarErro('Erro ao carregar formulário');
            }
        },

        /**
         * Configura event listener para fechar autocomplete ao clicar fora
         */
        configurarFechamentoAutocomplete() {
            document.addEventListener('click', (e) => {
                // Fechar lista de pessoas se clicar fora
                const pessoaContainer = e.target.closest('.autocomplete-container');
                if (!pessoaContainer || !pessoaContainer.querySelector('input[x-model="buscaPessoa"]')) {
                    this.mostrarListaPessoas = false;
                }

                // Fechar lista de advogados se clicar fora
                if (!pessoaContainer || !pessoaContainer.querySelector('input[x-model="buscaAdvogado"]')) {
                    this.mostrarListaAdvogados = false;
                }
            });
        },

        /**
         * Carregar dados auxiliares (varas, juízes, promotores, pessoas, advogados)
         */
        async carregarDadosAuxiliares() {
            try {
                // Carregar varas
                const resVaras = await fetch('/api/audiencias/varas');
                const dataVaras = await resVaras.json();
                if (dataVaras.success) {
                    this.varas = dataVaras.dados || [];
                }

                // Carregar juízes
                const resJuizes = await fetch('/api/audiencias/juizes');
                const dataJuizes = await resJuizes.json();
                if (dataJuizes.success) {
                    this.juizes = dataJuizes.dados || [];
                }

                // Carregar promotores
                const resPromotores = await fetch('/api/audiencias/promotores');
                const dataPromotores = await resPromotores.json();
                if (dataPromotores.success) {
                    this.promotores = dataPromotores.dados || [];
                }

                // Carregar pessoas
                const resPessoas = await fetch('/api/audiencias/pessoas');
                const dataPessoas = await resPessoas.json();
                if (dataPessoas.success) {
                    this.pessoas = dataPessoas.dados || [];
                }

                // Carregar advogados
                const resAdvogados = await fetch('/api/audiencias/advogados');
                const dataAdvogados = await resAdvogados.json();
                if (dataAdvogados.success) {
                    this.advogados = dataAdvogados.dados || [];
                }

                console.log('DEBUG_AUDIENCIAS: Dados auxiliares carregados');
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar dados auxiliares:', error);
            }
        },

        /**
         * Carregar audiência existente (modo edição)
         */
        async carregarAudiencia() {
            try {
                const res = await fetch(`/api/audiencias/${this.audienciaId}`);
                const data = await res.json();

                if (!data.success) {
                    throw new Error(data.message || 'Erro ao carregar audiência');
                }

                const audiencia = data.dados;

                // Converter data dd/MM/yyyy para yyyy-MM-dd
                const [dia, mes, ano] = audiencia.dataAudiencia.split('/');
                const dataFormatada = `${ano}-${mes}-${dia}`;

                // Preencher formulário
                this.form = {
                    numeroProcesso: audiencia.numeroProcesso || '',
                    varaId: audiencia.vara?.id || '',
                    dataAudiencia: dataFormatada || '',
                    horarioInicio: audiencia.horarioInicio || '',
                    horarioFim: audiencia.horarioFim || '',
                    tipoAudiencia: audiencia.tipoAudiencia || '',
                    formato: audiencia.formato || '',
                    status: audiencia.status || 'DESIGNADA',
                    juizId: audiencia.juiz?.id || '',
                    promotorId: audiencia.promotor?.id || '',
                    artigo: audiencia.artigo || '',
                    reuPreso: audiencia.reuPreso || false,
                    agendamentoTeams: audiencia.agendamentoTeams || false,
                    reconhecimento: audiencia.reconhecimento || false,
                    depoimentoEspecial: audiencia.depoimentoEspecial || false,
                    observacoes: audiencia.observacoes || '',
                    participantes: [],
                    representacoes: []
                };

                // Carregar participantes existentes
                await this.carregarParticipantesExistentes();

                console.log('DEBUG_AUDIENCIAS: Audiência carregada para edição com', this.form.participantes.length, 'participantes');
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar audiência:', error);
                this.mostrarErro('Erro ao carregar audiência');
            }
        },

        /**
         * Carregar participantes existentes da audiência (modo edição)
         */
        async carregarParticipantesExistentes() {
            try {
                const res = await fetch(`/api/audiencias/participacoes/audiencia/${this.audienciaId}`);
                const data = await res.json();

                if (!data.success) {
                    console.warn('DEBUG_AUDIENCIAS: Sem participantes para carregar');
                    return;
                }

                const participantes = data.dados || [];
                console.log('DEBUG_AUDIENCIAS: Carregando', participantes.length, 'participantes existentes');

                // Converter os participantes do formato DTO para o formato do formulário
                this.form.participantes = participantes.map(p => ({
                    pessoaId: p.pessoaId,
                    pessoaNome: p.pessoaNome,
                    tipoParticipacao: p.tipoParticipacao,
                    intimado: p.intimado || false,
                    observacoes: p.observacoes || '',
                    // Dados do advogado
                    advogadoId: p.advogadoId || null,
                    advogadoNome: p.advogadoNome || null,
                    advogadoOab: p.advogadoOab || null,
                    tipoRepresentacao: p.tipoRepresentacao || null
                }));

                console.log('DEBUG_AUDIENCIAS: Participantes carregados:', this.form.participantes);

            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao carregar participantes:', error);
            }
        },

        /**
         * Salvar audiência (criar ou atualizar)
         */
        async salvarAudiencia() {
            if (this.salvando) return;

            this.salvando = true;

            try {
                // Validar formulário
                if (!this.validarFormulario()) {
                    this.salvando = false;
                    return;
                }

                // Preparar dados
                const dados = this.prepararDados();
                console.log('DEBUG_AUDIENCIAS: Dados a serem enviados:', dados);
                console.log('DEBUG_AUDIENCIAS: Quantidade de participantes:', dados.participantes?.length || 0);

                // Definir método e URL
                const metodo = this.audienciaId ? 'PUT' : 'POST';
                const url = this.audienciaId
                    ? `/api/audiencias/${this.audienciaId}`
                    : '/api/audiencias';

                // Enviar requisição
                const res = await fetch(url, {
                    method: metodo,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(dados)
                });

                const data = await res.json();

                if (!data.success) {
                    throw new Error(data.message || 'Erro ao salvar audiência');
                }

                this.mostrarSucesso(
                    this.audienciaId
                        ? 'Audiência atualizada com sucesso!'
                        : 'Audiência cadastrada com sucesso!'
                );

                // Aguardar 1 segundo e redirecionar
                setTimeout(() => {
                    window.location.href = '/audiencias';
                }, 1000);

            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao salvar:', error);
                this.mostrarErro(error.message || 'Erro ao salvar audiência');
                this.salvando = false;
            }
        },

        /**
         * Validar formulário
         */
        validarFormulario() {
            if (!this.form.numeroProcesso.trim()) {
                this.mostrarErro('Número do processo é obrigatório');
                return false;
            }

            if (!this.form.varaId) {
                this.mostrarErro('Vara é obrigatória');
                return false;
            }

            if (!this.form.dataAudiencia) {
                this.mostrarErro('Data da audiência é obrigatória');
                return false;
            }

            if (!this.form.horarioInicio || !this.form.horarioFim) {
                this.mostrarErro('Horários de início e fim são obrigatórios');
                return false;
            }

            // Verificar conflito de horário
            if (this.temConflito) {
                const confirmar = confirm(
                    `⚠️ ATENÇÃO: Foi detectado conflito de horário!\n\n` +
                    `Já existe(m) ${this.conflitosEncontrados.length} audiência(s) neste horário.\n\n` +
                    `Deseja realmente salvar mesmo assim?`
                );

                if (!confirmar) {
                    return false;
                }
            }

            if (this.form.horarioInicio >= this.form.horarioFim) {
                this.mostrarErro('Horário de início deve ser anterior ao horário de fim');
                return false;
            }

            if (!this.form.tipoAudiencia) {
                this.mostrarErro('Tipo de audiência é obrigatório');
                return false;
            }

            if (!this.form.formato) {
                this.mostrarErro('Formato é obrigatório');
                return false;
            }

            return true;
        },

        /**
         * Preparar dados para envio
         */
        prepararDados() {
            // Converter data yyyy-MM-dd para dd/MM/yyyy
            const [ano, mes, dia] = this.form.dataAudiencia.split('-');
            const dataFormatada = `${dia}/${mes}/${ano}`;

            return {
                numeroProcesso: this.form.numeroProcesso.trim(),
                varaId: parseInt(this.form.varaId),
                dataAudiencia: dataFormatada,
                horarioInicio: this.form.horarioInicio,
                horarioFim: this.form.horarioFim,
                tipoAudiencia: this.form.tipoAudiencia,
                formato: this.form.formato,
                status: this.form.status,
                juizId: this.form.juizId ? parseInt(this.form.juizId) : null,
                promotorId: this.form.promotorId ? parseInt(this.form.promotorId) : null,
                artigo: this.form.artigo?.trim() || null,
                reuPreso: this.form.reuPreso || false,
                agendamentoTeams: this.form.agendamentoTeams || false,
                reconhecimento: this.form.reconhecimento || false,
                depoimentoEspecial: this.form.depoimentoEspecial || false,
                observacoes: this.form.observacoes?.trim() || null,
                participantes: this.form.participantes || []
            };
        },

        /**
         * Cancelar e voltar
         */
        cancelar() {
            if (confirm('Deseja realmente cancelar? Alterações não salvas serão perdidas.')) {
                window.location.href = '/audiencias';
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
        },

        // ====================================================================
        // GERENCIAMENTO DE PARTICIPANTES
        // ====================================================================

        /**
         * Adiciona participante à lista
         */
        adicionarParticipante() {
            console.log('DEBUG_AUDIENCIAS: adicionarParticipante() chamado');
            console.log('DEBUG_AUDIENCIAS: novoParticipante:', this.novoParticipante);

            // Validações
            if (!this.novoParticipante.pessoaId) {
                console.log('DEBUG_AUDIENCIAS: Erro - pessoaId não definido');
                this.mostrarErro('Selecione uma pessoa');
                return;
            }

            if (!this.novoParticipante.tipoParticipacao) {
                console.log('DEBUG_AUDIENCIAS: Erro - tipoParticipacao não definido');
                this.mostrarErro('Selecione o tipo de participação');
                return;
            }

            // Verificar duplicação
            const jaAdicionado = this.form.participantes.some(
                p => p.pessoaId === this.novoParticipante.pessoaId &&
                     p.tipoParticipacao === this.novoParticipante.tipoParticipacao
            );

            if (jaAdicionado) {
                console.log('DEBUG_AUDIENCIAS: Erro - participante já adicionado');
                this.mostrarErro('Esta pessoa já foi adicionada com este tipo de participação');
                return;
            }

            // Buscar dados da pessoa
            const pessoa = this.pessoas.find(p => p.id == this.novoParticipante.pessoaId);
            console.log('DEBUG_AUDIENCIAS: Pessoa encontrada:', pessoa);

            if (!pessoa) {
                console.log('DEBUG_AUDIENCIAS: Erro - pessoa não encontrada na lista');
                this.mostrarErro('Pessoa não encontrada');
                return;
            }

            // Criar objeto participante
            const participante = {
                pessoaId: this.novoParticipante.pessoaId,
                pessoaNome: pessoa.nome,
                tipoParticipacao: this.novoParticipante.tipoParticipacao,
                intimado: this.novoParticipante.intimado,
                observacoes: this.novoParticipante.observacoes || null,
                // Incluir advogado se foi selecionado
                advogadoId: this.novoParticipante.advogadoId || null,
                advogadoNome: this.novoParticipante.advogadoNome || null,
                advogadoOab: this.novoParticipante.advogadoOab || null,
                tipoRepresentacao: this.novoParticipante.tipoRepresentacao || null
            };

            console.log('DEBUG_AUDIENCIAS: Adicionando participante:', participante);
            console.log('DEBUG_AUDIENCIAS: form.participantes antes:', this.form.participantes.length);

            // Adicionar participante
            this.form.participantes.push(participante);

            console.log('DEBUG_AUDIENCIAS: form.participantes depois:', this.form.participantes.length);
            console.log('DEBUG_AUDIENCIAS: Lista completa:', this.form.participantes);

            // Limpar formulário
            this.novoParticipante = {
                pessoaId: '',
                pessoaNome: '',
                tipoParticipacao: '',
                intimado: false,
                observacoes: '',
                advogadoId: '',
                advogadoNome: '',
                advogadoOab: '',
                tipoRepresentacao: ''
            };

            // Limpar também os campos de busca
            this.buscaPessoa = '';
            this.buscaAdvogado = '';

            this.mostrarSucesso('Participante adicionado');
        },

        /**
         * Remove participante da lista
         */
        removerParticipante(index) {
            this.form.participantes.splice(index, 1);
            this.mostrarSucesso('Participante removido');
        },

        // ====================================================================
        // GERENCIAMENTO DE REPRESENTAÇÃO
        // ====================================================================

        /**
         * Adiciona representação de advogado à lista
         */
        adicionarRepresentacao() {
            // Validações
            if (!this.novaRepresentacao.advogadoId) {
                this.mostrarErro('Selecione um advogado');
                return;
            }

            if (!this.novaRepresentacao.clienteId) {
                this.mostrarErro('Selecione a pessoa representada');
                return;
            }

            if (!this.novaRepresentacao.tipoRepresentacao) {
                this.mostrarErro('Selecione o tipo de representação');
                return;
            }

            // Verificar duplicação
            const jaAdicionado = this.form.representacoes.some(
                r => r.advogadoId === this.novaRepresentacao.advogadoId &&
                     r.clienteId === this.novaRepresentacao.clienteId
            );

            if (jaAdicionado) {
                this.mostrarErro('Este advogado já representa esta pessoa');
                return;
            }

            // Buscar dados do advogado e cliente
            const advogado = this.advogados.find(a => a.id == this.novaRepresentacao.advogadoId);
            const cliente = this.pessoas.find(p => p.id == this.novaRepresentacao.clienteId);

            if (!advogado || !cliente) {
                this.mostrarErro('Advogado ou cliente não encontrado');
                return;
            }

            // Adicionar representação
            this.form.representacoes.push({
                advogadoId: this.novaRepresentacao.advogadoId,
                advogadoNome: advogado.nome,
                advogadoOab: advogado.oab,
                clienteId: this.novaRepresentacao.clienteId,
                clienteNome: cliente.nome,
                tipoRepresentacao: this.novaRepresentacao.tipoRepresentacao
            });

            // Limpar formulário
            this.novaRepresentacao = {
                advogadoId: '',
                clienteId: '',
                tipoRepresentacao: ''
            };

            this.mostrarSucesso('Representação adicionada');
            console.log('DEBUG_AUDIENCIAS: Representação adicionada:', this.form.representacoes);
        },

        /**
         * Remove representação da lista
         */
        removerRepresentacao(index) {
            this.form.representacoes.splice(index, 1);
            this.mostrarSucesso('Representação removida');
        },

        /**
         * Alterna status de intimação de um participante
         */
        alternarIntimacao(index) {
            this.form.participantes[index].intimado = !this.form.participantes[index].intimado;
            const status = this.form.participantes[index].intimado ? 'intimado' : 'não intimado';
            this.mostrarSucesso(`Participante marcado como ${status}`);
        },

        // ====================================================================
        // AUTOCOMPLETE DE PESSOAS E ADVOGADOS
        // ====================================================================

        /**
         * Seleciona pessoa do autocomplete
         */
        selecionarPessoa(pessoaId) {
            this.novoParticipante.pessoaId = pessoaId;
            const pessoa = this.pessoas.find(p => p.id === pessoaId);
            if (pessoa) {
                // Mostrar nome da pessoa selecionada no input
                this.buscaPessoa = pessoa.nome;
                this.novoParticipante.pessoaNome = pessoa.nome;
            }
            // Fechar lista
            this.mostrarListaPessoas = false;
        },

        /**
         * Seleciona advogado do autocomplete
         */
        selecionarAdvogado(advogadoId) {
            this.novoParticipante.advogadoId = advogadoId;
            const advogado = this.advogados.find(a => a.id === advogadoId);
            if (advogado) {
                // Mostrar nome do advogado selecionado no input
                this.buscaAdvogado = `${advogado.nome} (${advogado.oab})`;
                this.novoParticipante.advogadoNome = advogado.nome;
                this.novoParticipante.advogadoOab = advogado.oab;
            }
            // Fechar lista
            this.mostrarListaAdvogados = false;
        },

        /**
         * Abre modal/tela para adicionar nova pessoa
         */
        abrirAdicionarPessoa() {
            // Abrir em nova aba a tela de cadastro de pessoas com nome pré-preenchido
            const nomeEncoded = encodeURIComponent(this.buscaPessoa);
            window.open(`/audiencias/pessoas?nome=${nomeEncoded}`, '_blank');
            this.mostrarSucesso('Após cadastrar a pessoa, volte aqui e clique em 🔄 para atualizar a lista');
        },

        /**
         * Abre modal/tela para adicionar novo advogado
         */
        abrirAdicionarAdvogado() {
            // Abrir em nova aba a tela de cadastro de advogados com nome pré-preenchido
            const nomeEncoded = encodeURIComponent(this.buscaAdvogado);
            window.open(`/audiencias/advogados?nome=${nomeEncoded}`, '_blank');
            this.mostrarSucesso('Após cadastrar o advogado, volte aqui e clique em 🔄 para atualizar a lista');
        },

        /**
         * Recarrega lista de pessoas
         */
        async recarregarPessoas() {
            try {
                const resPessoas = await fetch('/api/audiencias/pessoas');
                const dataPessoas = await resPessoas.json();
                if (dataPessoas.success) {
                    this.pessoas = dataPessoas.dados || [];
                    this.mostrarSucesso('Lista de pessoas atualizada');
                }
            } catch (error) {
                console.error('Erro ao recarregar pessoas:', error);
                this.mostrarErro('Erro ao recarregar lista de pessoas');
            }
        },

        /**
         * Recarrega lista de advogados
         */
        async recarregarAdvogados() {
            try {
                const resAdvogados = await fetch('/api/audiencias/advogados');
                const dataAdvogados = await resAdvogados.json();
                if (dataAdvogados.success) {
                    this.advogados = dataAdvogados.dados || [];
                    this.mostrarSucesso('Lista de advogados atualizada');
                }
            } catch (error) {
                console.error('Erro ao recarregar advogados:', error);
                this.mostrarErro('Erro ao recarregar lista de advogados');
            }
        },

        /**
         * Busca pessoa pelo objeto completo (usado nos cards)
         */
        buscarPessoaPorId(pessoaId) {
            return this.pessoas.find(p => p.id == pessoaId);
        },

        /**
         * Busca advogado pelo objeto completo (usado nos cards)
         */
        buscarAdvogadoPorId(advogadoId) {
            return this.advogados.find(a => a.id == advogadoId);
        },

        // ====================================================================
        // VERIFICAÇÃO DE CONFLITOS AUTOMÁTICA
        // ====================================================================

        /**
         * Verifica conflitos de horário automaticamente com debounce
         */
        verificarConflitosAutomatico() {
            // Limpar timeout anterior
            if (this.timeoutConflito) {
                clearTimeout(this.timeoutConflito);
            }

            // Verificar se tem os dados mínimos necessários
            if (!this.form.dataAudiencia || !this.form.horarioInicio || !this.form.varaId) {
                this.temConflito = false;
                this.conflitosEncontrados = [];
                return;
            }

            // Calcular duração em minutos
            let duracaoMinutos = 60; // Padrão
            if (this.form.horarioFim && this.form.horarioInicio) {
                const [horaIni, minIni] = this.form.horarioInicio.split(':').map(Number);
                const [horaFim, minFim] = this.form.horarioFim.split(':').map(Number);
                duracaoMinutos = (horaFim * 60 + minFim) - (horaIni * 60 + minIni);
            }

            // Aguardar 800ms após última mudança para verificar
            this.timeoutConflito = setTimeout(async () => {
                await this.verificarConflitos(duracaoMinutos);
            }, 800);
        },

        /**
         * Verifica conflitos na API
         */
        async verificarConflitos(duracaoMinutos) {
            try {
                this.verificandoConflito = true;

                // Converter data para formato brasileiro
                const dataFormatada = this.converterDataParaBR(this.form.dataAudiencia);

                // Montar query params
                const params = new URLSearchParams({
                    data: dataFormatada,
                    horarioInicio: this.form.horarioInicio + ':00',
                    duracao: duracaoMinutos,
                    varaId: this.form.varaId
                });

                // Se estiver editando, excluir própria audiência da verificação
                if (this.audienciaId) {
                    params.append('audienciaIdExcluir', this.audienciaId);
                }

                console.log('DEBUG_AUDIENCIAS: Verificando conflitos...', params.toString());

                const response = await fetch(`/api/audiencias/conflitos?${params.toString()}`);
                const data = await response.json();

                if (data.success) {
                    this.conflitosEncontrados = data.dados || [];
                    this.temConflito = this.conflitosEncontrados.length > 0;

                    if (this.temConflito) {
                        console.log('DEBUG_AUDIENCIAS: Conflitos encontrados:', this.conflitosEncontrados.length);
                    }
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao verificar conflitos:', error);
            } finally {
                this.verificandoConflito = false;
            }
        },

        /**
         * Converte data de yyyy-MM-dd para dd/MM/yyyy
         */
        converterDataParaBR(dataISO) {
            if (!dataISO) return '';
            const partes = dataISO.split('-');
            return `${partes[2]}/${partes[1]}/${partes[0]}`;
        },

        /**
         * Observa mudanças nos campos relevantes para verificar conflitos
         */
        observarCamposConflito() {
            // Usar Alpine's $watch para reagir a mudanças
            this.$watch('form.dataAudiencia', () => this.verificarConflitosAutomatico());
            this.$watch('form.horarioInicio', () => this.verificarConflitosAutomatico());
            this.$watch('form.horarioFim', () => this.verificarConflitosAutomatico());
            this.$watch('form.varaId', () => this.verificarConflitosAutomatico());
        },

        // ====================================================================
        // PREENCHER DADOS DE HORÁRIO SELECIONADO
        // ====================================================================

        /**
         * Preenche formulário com dados do horário selecionado na busca de horários livres
         */
        preencherDadosHorarioSelecionado() {
            try {
                // Verificar se há dados no sessionStorage
                const data = sessionStorage.getItem('novaAudienciaData');
                const horarioInicio = sessionStorage.getItem('novaAudienciaHorario');
                const duracaoMinutos = sessionStorage.getItem('novaAudienciaDuracao');
                const varaId = sessionStorage.getItem('novaAudienciaVara');

                if (data && horarioInicio && duracaoMinutos) {
                    console.log('DEBUG_AUDIENCIAS: Preenchendo formulário com horário selecionado');

                    // Preencher data (já está em formato yyyy-MM-dd)
                    this.form.dataAudiencia = data;

                    // Preencher horário de início (formato HH:mm)
                    this.form.horarioInicio = horarioInicio;

                    // Calcular horário de término
                    const [hora, minuto] = horarioInicio.split(':').map(Number);
                    const totalMinutos = hora * 60 + minuto + parseInt(duracaoMinutos);
                    const horaFim = Math.floor(totalMinutos / 60);
                    const minutoFim = totalMinutos % 60;
                    this.form.horarioFim = `${String(horaFim).padStart(2, '0')}:${String(minutoFim).padStart(2, '0')}`;

                    // Preencher vara se foi especificada
                    if (varaId) {
                        this.form.varaId = varaId;
                    }

                    console.log('DEBUG_AUDIENCIAS: Campos preenchidos:', {
                        data: this.form.dataAudiencia,
                        horarioInicio: this.form.horarioInicio,
                        horarioFim: this.form.horarioFim,
                        varaId: this.form.varaId
                    });

                    // Limpar sessionStorage
                    sessionStorage.removeItem('novaAudienciaData');
                    sessionStorage.removeItem('novaAudienciaHorario');
                    sessionStorage.removeItem('novaAudienciaDuracao');
                    sessionStorage.removeItem('novaAudienciaVara');

                    // Mostrar mensagem de sucesso
                    this.mostrarSucesso('Horário selecionado preenchido automaticamente!');
                }
            } catch (error) {
                console.error('DEBUG_AUDIENCIAS: Erro ao preencher dados do horário:', error);
            }
        }
    }
}
