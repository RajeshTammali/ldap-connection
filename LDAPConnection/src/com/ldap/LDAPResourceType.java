package com.ldap;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LDAPResourceType {
	public static void main(String[] args) throws NamingException {
		// updating
		String LDAP_URL = "ldap://localhost:10389/dc=WSO2,dc=ORG";
		String LDAP_USER = "uid=admin,ou=system";
		String LDAP_PASSWORD = "admin";
		String userid = "";
		int index;
		String resourceType = "User";
		Iterator itr = null;
		String subTree = "";
		List al = new ArrayList();
		al.add("ou=users"); // users ou
		al.add("ou=users,ou=mycompany.com"); // tenant ou
		System.out.println(al);
		final Log log = LogFactory.getLog(LDAPResourceType.class);
		log.info("Trying to connect ldap");
		Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, LDAP_URL); // please modify the URL accordingly
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, LDAP_USER); // using admin as the user
		env.put(Context.SECURITY_CREDENTIALS, LDAP_PASSWORD); // using admin credentials to connect
		DirContext context = new InitialDirContext(env);
		itr = al.iterator();
		while (itr.hasNext()) {
			subTree = (String) itr.next();
			DirContext groupCx = (DirContext) context.lookup(subTree);
			NamingEnumeration<Binding> groups = groupCx.listBindings("");
			while (groups.hasMore()) {
				String bindingName = groups.next().getName();
				Attributes groupAttributes = groupCx.getAttributes(bindingName);
				Attribute cn = groupAttributes.get("cn");
				log.info(cn);
				Attribute uid = groupAttributes.get("uid");
				log.info(uid);
				if (uid != null) {
					userid = uid.toString();
					index = userid.indexOf(":");
					index = index + 1;
					userid = userid.substring(index);
					userid = userid.trim();
					try {
						log.info("Trying to update attribute for the user" + cn);
						Attribute attribute = new BasicAttribute("resourceType", resourceType);
						ModificationItem[] item = new ModificationItem[1];
						item[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute);

						context.modifyAttributes("uid=" + userid + "," + subTree, item);
					} catch (NamingException exception) {
						log.error(exception);
					}
				}

				else {
					log.error("uid cannot be null.found null for the user " + cn);
				}
			}
		}
	}
}