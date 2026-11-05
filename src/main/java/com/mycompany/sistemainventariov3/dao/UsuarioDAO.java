package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Usuario;
import javax.persistence.EntityManager;

/**
 * DAO para la entidad Usuario
 */
public class UsuarioDAO extends DAOGenerico<Usuario, Integer> {
    
    public UsuarioDAO() {
        super(Usuario.class);
    }
    
    /**
     * Buscar usuario por username
     */
    public Usuario buscarPorUsername(String username) {
        EntityManager em = getEM();
        try {
            return (Usuario) em.createQuery("FROM Usuario WHERE username = :username")
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    /**
     * Validar credenciales de usuario
     */
    public Usuario validarCredenciales(String username, String passwordHash) {
        EntityManager em = getEM();
        try {
            return (Usuario) em.createQuery("FROM Usuario WHERE username = :username AND password = :password")
                    .setParameter("username", username)
                    .setParameter("password", passwordHash)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
}
