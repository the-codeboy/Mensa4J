package com.github.codeboy;

import com.github.codeboy.api.Mensa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class OpenMensaTest {

    @Test
    public void testCanteens(){
        OpenMensa.getInstance().reloadCanteens();
        Collection<Mensa>mensas=OpenMensa.getInstance().getAllCanteens();
        Assertions.assertTrue(mensas.size()>0);
    }
}