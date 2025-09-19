package com.vaadin.starter.bakery.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.List;

/**
 * Componente {@link Grid} simples para demonstração/experimentação.
 * <p>Bean Spring com escopo de UI ({@link UIScope}); um botão por linha
 * que abre um {@code RouteChangingDialog} ao clicar.</p>
 */
@SpringComponent
@UIScope
public class GridComponent extends Grid<Integer> {

    /** Constrói o grid, define itens e a coluna de botões. */
    public GridComponent() {
        setItems(List.of(0, 1));           // <- aqui está o fix
        addComponentColumn(this::createButton);
    }

    /**
     * Cria o botão para a linha correspondente.
     * @param i valor da linha
     * @return botão que abre um {@code RouteChangingDialog}
     */
    private Button createButton(Integer i) {
        return new Button("Test Button " + i, e -> {
            RouteChangingDialog dialog = new RouteChangingDialog();
            dialog.open();
        });
    }
}
