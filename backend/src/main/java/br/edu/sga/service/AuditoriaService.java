package br.edu.sga.service;

import br.edu.sga.entity.LogAuditoria;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuditoriaService {
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrar(Long usuarioId, String nome, String email, String perfil,
                          String acao, String metodo, String rota, int statusHttp) {
        LogAuditoria log = new LogAuditoria();
        log.usuarioId = usuarioId;
        log.usuarioNome = limitar(nome, 255);
        log.usuarioEmail = limitar(email, 255);
        log.perfil = limitar(perfil, 50);
        log.acao = limitar(acao, 30);
        log.metodo = limitar(metodo, 10);
        log.rota = limitar(rota, 500);
        log.statusHttp = statusHttp;
        log.sucesso = statusHttp >= 200 && statusHttp < 400;
        log.persist();
    }

    private String limitar(String valor, int tamanho) {
        if (valor == null) return null;
        return valor.length() <= tamanho ? valor : valor.substring(0, tamanho);
    }
}
