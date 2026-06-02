package com.tu.backend.externalresource.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExternalResourceBootstrapRunnerTest {

    @Test
    void createsBookTypeWhenMissing() {
        ResourceTypeRepository repository = mock(ResourceTypeRepository.class);
        when(repository.findByCode("book")).thenReturn(Optional.empty());
        when(repository.findByName("图书")).thenReturn(Optional.empty());
        when(repository.save(any(ResourceTypeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new ExternalResourceBootstrapRunner(repository).run(null);

        ArgumentCaptor<ResourceTypeEntity> captor = ArgumentCaptor.forClass(ResourceTypeEntity.class);
        verify(repository).save(captor.capture());
        ResourceTypeEntity saved = captor.getValue();
        assertThat(saved.getId()).startsWith("rt-");
        assertThat(saved.getCode()).isEqualTo("book");
        assertThat(saved.getName()).isEqualTo("图书");
        assertThat(saved.getIcon()).isEqualTo("book");
        assertThat(saved.getIdentityFieldKey()).isEqualTo("isbn");
        assertThat(saved.getIdentityFieldLabel()).isEqualTo("ISBN");
    }

    @Test
    void normalizesExistingBookTypeByName() {
        ResourceTypeRepository repository = mock(ResourceTypeRepository.class);
        ResourceTypeEntity existing = new ResourceTypeEntity();
        existing.setId("rt-existing");
        existing.setCode("legacy-book");
        existing.setName("图书");
        existing.setIdentityFieldKey("customId");
        existing.setIdentityFieldLabel("自定义编号");
        when(repository.findByCode("book")).thenReturn(Optional.empty());
        when(repository.findByName("图书")).thenReturn(Optional.of(existing));
        when(repository.save(any(ResourceTypeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new ExternalResourceBootstrapRunner(repository).run(null);

        assertThat(existing.getCode()).isEqualTo("book");
        assertThat(existing.getName()).isEqualTo("图书");
        assertThat(existing.getIdentityFieldKey()).isEqualTo("customId");
        assertThat(existing.getIdentityFieldLabel()).isEqualTo("自定义编号");
        verify(repository).save(existing);
    }
}
