package uk.co.markg.paperclip.listener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Listeners {

    private static final Logger logger = LoggerFactory.getLogger(Listeners.class);

    public static Object[] getListeners(String listenerPackage) {
        var reflections = new Reflections(listenerPackage);
        Set<Class<? extends ListenerAdapter>> classes =
                new HashSet<Class<? extends ListenerAdapter>>();
        try {
            classes = reflections.getSubTypesOf(ListenerAdapter.class);
        } catch (ReflectionsException e) {
            logger.warn("Failed to find classes. Package may be invalid.");
            return new Object[0];
        }
        ListenerAdapter[] listeners = new ListenerAdapter[classes.size()];
        Iterator<Class<? extends ListenerAdapter>> itr = classes.iterator();
        int i = 0;
        while (itr.hasNext()) {
            var obj = itr.next();
            try {
                logger.info("Added {} Listener", obj.getName());
                listeners[i] = (ListenerAdapter) Class.forName(obj.getName())
                        .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                logger.error("Error", e);
            }
            i++;
        }
        return listeners;
    }
}
