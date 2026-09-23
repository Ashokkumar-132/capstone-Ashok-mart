package com.ashokmart.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.sql.SQLException;

/** Creates infrastructure once at web-application startup and closes it at shutdown. */
@WebListener
public final class DatabaseContextListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        DatabaseConnectionPool pool = DatabaseConnectionPool.createDefault();
        try {
            DatabaseInitializer.initialize(pool);
            event.getServletContext().setAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE, pool);
            LOGGER.info("AshokMart database infrastructure started");
        } catch (SQLException | IOException exception) {
            pool.close();
            throw new IllegalStateException("AshokMart database startup failed", exception);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object attribute = event.getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (attribute instanceof DatabaseConnectionPool pool) {
            pool.close();
            event.getServletContext().removeAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        }
        LOGGER.info("AshokMart database infrastructure stopped");
    }
}
