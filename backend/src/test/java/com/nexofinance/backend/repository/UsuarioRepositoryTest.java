package com.nexofinance.backend.repository;

import com.nexofinance.backend.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Deve salvar e buscar usuario por email com sucesso")
    void deveSalvarEBuscarUsuarioPorEmail() {
        Usuario usuario = new Usuario("Ítalo Teste", "italo.teste@nexofinance.com", "hash_seguro_123");
        Usuario salvo = usuarioRepository.save(usuario);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getCreatedAt()).isNotNull();
        assertThat(salvo.getUpdatedAt()).isNotNull();

        Optional<Usuario> encontrado = usuarioRepository.findByEmail("italo.teste@nexofinance.com");
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Ítalo Teste");
        assertThat(encontrado.get().getSenhaHash()).isEqualTo("hash_seguro_123");
    }

    @Test
    @DisplayName("Deve verificar existencia de usuario por email")
    void deveVerificarExistenciaPorEmail() {
        Usuario usuario = new Usuario("Outro User", "existente@nexofinance.com", "senha");
        usuarioRepository.save(usuario);

        boolean existe = usuarioRepository.existsByEmail("existente@nexofinance.com");
        boolean naoExiste = usuarioRepository.existsByEmail("naoexiste@nexofinance.com");

        assertThat(existe).isTrue();
        assertThat(naoExiste).isFalse();
    }
}
