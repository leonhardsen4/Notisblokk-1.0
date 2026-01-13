package com.notisblokk.service;

import com.notisblokk.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Serviço de envio de emails usando JavaMail.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Enviar email de confirmação de cadastro</li>
 *   <li>Enviar email de recuperação de senha</li>
 *   <li>Enviar alertas de prazos de tarefas</li>
 *   <li>Enviar avisos de expiração de senha</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final Session session;

    /**
     * Construtor que inicializa a sessão SMTP com as configurações do AppConfig.
     */
    public EmailService() {
        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.getEmailSmtpHost());
        props.put("mail.smtp.port", AppConfig.getEmailSmtpPort());
        props.put("mail.smtp.auth", AppConfig.getEmailSmtpAuth());
        props.put("mail.smtp.starttls.enable", AppConfig.getEmailSmtpStartTls());

        final String username = AppConfig.getEmailUsername();
        final String password = AppConfig.getEmailPassword();

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        logger.info("EmailService inicializado com SMTP: {}:{}",
                   AppConfig.getEmailSmtpHost(), AppConfig.getEmailSmtpPort());
    }

    /**
     * Envia um email genérico.
     *
     * @param to destinatário
     * @param subject assunto
     * @param htmlBody corpo do email em HTML
     * @throws Exception se houver erro no envio
     */
    private void sendEmail(String to, String subject, String htmlBody) throws Exception {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(AppConfig.getEmailFrom(), AppConfig.getEmailFromName()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
            logger.info("Email enviado com sucesso para: {}", to);

        } catch (Exception e) {
            logger.error("Erro ao enviar email para {}", to, e);
            throw new Exception("Falha ao enviar email: " + e.getMessage());
        }
    }

    /**
     * Envia email de confirmação de cadastro.
     *
     * @param email email do usuário
     * @param nomeUsuario nome do usuário
     * @param token token de verificação
     * @throws Exception se houver erro no envio
     */
    public void enviarEmailConfirmacao(String email, String nomeUsuario, String token) throws Exception {
        String subject = "Confirme seu cadastro no " + AppConfig.getAppName();

        String body = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4A90E2; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4A90E2; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p>Obrigado por se cadastrar no %s.</p>
                        <p>Para confirmar seu email e ativar sua conta, clique no botão abaixo:</p>
                        <p style="text-align: center;">
                            <a href="http://localhost:%d/auth/verificar-email?token=%s" class="button">
                                Confirmar Email
                            </a>
                        </p>
                        <p>Ou copie e cole este link no seu navegador:</p>
                        <p style="word-break: break-all; background-color: #fff; padding: 10px; border: 1px solid #ddd;">
                            http://localhost:%d/auth/verificar-email?token=%s
                        </p>
                        <p><strong>Este link expira em 24 horas.</strong></p>
                    </div>
                    <div class="footer">
                        <p>Se você não se cadastrou no %s, ignore este email.</p>
                        <p>© 2025 %s - Todos os direitos reservados</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            AppConfig.getAppName(),
            nomeUsuario,
            AppConfig.getAppName(),
            AppConfig.getServerPort(),
            token,
            AppConfig.getServerPort(),
            token,
            AppConfig.getAppName(),
            AppConfig.getAppName()
        );

        sendEmail(email, subject, body);
    }

    /**
     * Envia email de recuperação de senha.
     *
     * @param email email do usuário
     * @param nomeUsuario nome do usuário
     * @param token token de recuperação
     * @throws Exception se houver erro no envio
     */
    public void enviarEmailRecuperacaoSenha(String email, String nomeUsuario, String token) throws Exception {
        String subject = "Recuperação de Senha - " + AppConfig.getAppName();

        String body = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #F97316; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #F97316; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    .warning { background-color: #FEF3C7; border-left: 4px solid #F59E0B; padding: 10px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Recuperação de Senha</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p>Recebemos uma solicitação para redefinir a senha da sua conta no %s.</p>
                        <p>Para criar uma nova senha, clique no botão abaixo:</p>
                        <p style="text-align: center;">
                            <a href="http://localhost:%d/auth/nova-senha?token=%s" class="button">
                                Redefinir Senha
                            </a>
                        </p>
                        <p>Ou copie e cole este link no seu navegador:</p>
                        <p style="word-break: break-all; background-color: #fff; padding: 10px; border: 1px solid #ddd;">
                            http://localhost:%d/auth/nova-senha?token=%s
                        </p>
                        <div class="warning">
                            <strong>⚠️ Atenção:</strong> Este link expira em 1 hora por motivos de segurança.
                        </div>
                        <p>Se você não solicitou a recuperação de senha, ignore este email. Sua senha permanecerá inalterada.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 %s - Todos os direitos reservados</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            nomeUsuario,
            AppConfig.getAppName(),
            AppConfig.getServerPort(),
            token,
            AppConfig.getServerPort(),
            token,
            AppConfig.getAppName()
        );

        sendEmail(email, subject, body);
    }

    /**
     * Envia email de alerta de prazo de tarefa.
     *
     * @param email email do usuário
     * @param nomeUsuario nome do usuário
     * @param tituloNota título da tarefa
     * @param prazoFinal prazo final formatado (dd/MM/yyyy)
     * @param diasRestantes quantos dias faltam
     * @throws Exception se houver erro no envio
     */
    public void enviarEmailAlertaNota(String email, String nomeUsuario, String tituloNota,
                                      String prazoFinal, int diasRestantes) throws Exception {
        String urgencia;
        String cor;

        if (diasRestantes < 0) {
            urgencia = "CRÍTICO - Tarefa Atrasada";
            cor = "#DC2626";
        } else if (diasRestantes == 0) {
            urgencia = "CRÍTICO - Vence HOJE";
            cor = "#DC2626";
        } else if (diasRestantes <= 3) {
            urgencia = "URGENTE";
            cor = "#F97316";
        } else if (diasRestantes <= 5) {
            urgencia = "ATENÇÃO";
            cor = "#F59E0B";
        } else {
            urgencia = "AVISO";
            cor = "#3B82F6";
        }

        String subject = String.format("[%s] Alerta: %s", urgencia, tituloNota);

        String mensagem = diasRestantes < 0
            ? String.format("Esta tarefa está atrasada há %d dia(s)!", Math.abs(diasRestantes))
            : diasRestantes == 0
            ? "Esta tarefa vence HOJE!"
            : String.format("Faltam apenas %d dia(s) para o prazo!", diasRestantes);

        String body = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: %s; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .nota-info { background-color: white; padding: 15px; border-left: 4px solid %s; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⚠️ Alerta de Prazo</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p><strong>%s</strong></p>
                        <div class="nota-info">
                            <h3>%s</h3>
                            <p><strong>Prazo final:</strong> %s</p>
                            <p><strong>Nível de urgência:</strong> %s</p>
                        </div>
                        <p style="text-align: center;">
                            <a href="http://localhost:%d/tarefas" class="button">
                                Ver Tarefa
                            </a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Você está recebendo este email porque habilitou alertas por email no %s.</p>
                        <p>© 2025 %s - Todos os direitos reservados</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            cor, cor, cor,
            nomeUsuario,
            mensagem,
            tituloNota,
            prazoFinal,
            urgencia,
            AppConfig.getServerPort(),
            AppConfig.getAppName(),
            AppConfig.getAppName()
        );

        sendEmail(email, subject, body);
    }

    /**
     * Envia email de aviso de expiração de senha.
     *
     * @param email email do usuário
     * @param nomeUsuario nome do usuário
     * @param diasRestantes quantos dias faltam para expirar
     * @throws Exception se houver erro no envio
     */
    public void enviarEmailAvisoExpiracaoSenha(String email, String nomeUsuario, int diasRestantes) throws Exception {
        String subject = String.format("Aviso: Sua senha expira em %d dia(s)", diasRestantes);

        String body = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #F59E0B; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-radius: 0 0 5px 5px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #F59E0B; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .warning { background-color: #FEF3C7; border-left: 4px solid #F59E0B; padding: 10px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⏰ Aviso de Segurança</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <div class="warning">
                            <strong>⚠️ Sua senha expira em %d dia(s)!</strong>
                        </div>
                        <p>Por motivos de segurança, o %s requer que você altere sua senha a cada %d meses.</p>
                        <p>Após a expiração, sua conta será desativada automaticamente até que você crie uma nova senha.</p>
                        <p style="text-align: center;">
                            <a href="http://localhost:%d/perfil" class="button">
                                Alterar Senha Agora
                            </a>
                        </p>
                        <p><strong>Dicas para criar uma senha segura:</strong></p>
                        <ul>
                            <li>Use no mínimo 8 caracteres</li>
                            <li>Combine letras maiúsculas e minúsculas</li>
                            <li>Inclua números e caracteres especiais</li>
                            <li>Não reutilize senhas antigas</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>© 2025 %s - Todos os direitos reservados</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            nomeUsuario,
            diasRestantes,
            AppConfig.getAppName(),
            AppConfig.getSecurityPasswordExpirationMonths(),
            AppConfig.getServerPort(),
            AppConfig.getAppName()
        );

        sendEmail(email, subject, body);
    }
}
