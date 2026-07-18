package br.edu.sga.service;

import br.edu.sga.entity.MigracaoTurmaOfertaPendencia;
import br.edu.sga.entity.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class PendenciaMigracaoService {
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrarVinculoProfessor(Usuario usuario, String tipo, String email, String motivo) {
        String divergencia = "usuarioId=" + usuario.id + ", email=" + email;
        if (MigracaoTurmaOfertaPendencia.count(
                "tipoPendencia = ?1 and divergencias = ?2 and resolvida = false", tipo, divergencia) > 0) {
            return;
        }
        MigracaoTurmaOfertaPendencia pendencia = new MigracaoTurmaOfertaPendencia();
        pendencia.tipoPendencia = tipo;
        pendencia.camposAusentes = "Professor.usuario";
        pendencia.divergencias = divergencia;
        pendencia.acaoAutomatica = "Nenhum vinculo realizado";
        pendencia.motivoIntervencaoManual = motivo;
        pendencia.dataCriacao = LocalDateTime.now();
        pendencia.persist();
    }
}
