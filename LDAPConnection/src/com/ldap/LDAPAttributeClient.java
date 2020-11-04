package com.ldap;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class LDAPAttributeClient {

	private static String LDAP_URL = "ldap://localhost:10389";
	private static String LDAP_USER = "uid=admin,ou=system";
	private static String LDAP_PASSWORD = "admin";
	private static String MODIFIED_ATTRIBUTE = "ref";
	private static String NEW_ATTRIBUTE = "resourceType";
	private static String USERNAME_ATTRIBUTE = "uid";
	private static String MODIFIED_ATTRIBUTE_VALUE = "User";

	private static Hashtable<String, String> env = new Hashtable<>();

	public static void main(String[] args) throws NamingException {

		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, LDAP_URL); // please modify the URL accordingly
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, LDAP_USER); // using admin as the user
		env.put(Context.SECURITY_CREDENTIALS, LDAP_PASSWORD); // using admin credentials to connect

		if (args != null && args.length > 0) {
			if ("5.7.0".equals(args[0]) || "570".equals(args[0])) {

				System.out.println("=====> Invalid attribute is going to delete from 5.7.0 Version ========> ");

				Set<String> userDNs = getDNsWithModifiedValue(MODIFIED_ATTRIBUTE, true);

				if (userDNs.size() > 0) {
					deleteAttributeValue(userDNs, MODIFIED_ATTRIBUTE);
				}

				System.out.println("=====> Tool was successful. Please check error logs.  Now you can export the LDAP "
						+ "date as LDIF file ========> ");

				return;
			}

			if ("5.9.0".equals(args[0]) || "590".equals(args[0])) {

				System.out.println("=====> Attribute Value is going to add for 5.9.0 Version ========> ");

				Set<String> userDNs = getDNsWithModifiedValue(NEW_ATTRIBUTE, false);

				if (userDNs.size() > 0) {
					updateAttributeValue(userDNs, NEW_ATTRIBUTE);
				}

				System.out.println("=====> Tool was successful. Please check error logs.  Now you can export the LDAP "
						+ "date as LDIF file ========> ");

				return;
			}

		}

		System.out.println("=====> Please provide product Version as argument ========> ");

	}

	private static Set<String> getDNsWithModifiedValue(String modifiedAttribute, boolean onlyWithModifiedAttribute) {

		System.out.println("=====> Retrieving LDAP DNs for given attribute ========> ");

		DirContext context = null;
		Set<String> userDNList = new HashSet<>();

		try {
			context = new InitialDirContext(env);

			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setReturningAttributes(new String[] { modifiedAttribute, USERNAME_ATTRIBUTE });
			NamingEnumeration<SearchResult> groups = context.search("dc=WSO2,dc=ORG", "(objectClass=person)", controls);

			while (groups.hasMore()) {
				SearchResult result = groups.next();
				String bindingName = result.getNameInNamespace();
				System.out.println("=====> User DN : " + bindingName);
				Attributes groupAttributes = result.getAttributes();
				Attribute modifiedAttributeValue = groupAttributes.get(modifiedAttribute);
				System.out.println("=====> User " + modifiedAttribute + " Attribute Value : " + modifiedAttributeValue);

				if (onlyWithModifiedAttribute) {
					if (modifiedAttributeValue != null) {
						if (modifiedAttributeValue.get().toString().equals(MODIFIED_ATTRIBUTE_VALUE)) {
							userDNList.add(bindingName);
						} else {
							System.out.println("=====>  WARN : Unknown attribute value for User : " + bindingName);
						}
					}
				} else {
					userDNList.add(bindingName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (context != null) {
				try {
					context.close();
				} catch (NamingException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("=====> LDAP DNs are retrieved successfully  ========> " + userDNList);

		return userDNList;
	}

	private static void deleteAttributeValue(Set<String> userDNList, String modifiedAttribute) throws NamingException {

		System.out.println("=====> Removing Attribute from given DNs ========> ");

		DirContext context = null;

		try {
			context = new InitialDirContext(env);

			for (String userDN : userDNList) {
				Attribute attribute = new BasicAttribute(modifiedAttribute);
				ModificationItem[] item = new ModificationItem[1];
				item[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attribute);
				context.modifyAttributes(userDN, item);

				System.out.println("=====> Attribute is removed for User DN : " + userDN);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (context != null) {
				try {
					context.close();
				} catch (NamingException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("=====> Attribute is removed from given DNs successfully ========> ");

	}

	private static void updateAttributeValue(Set<String> userDNList, String modifiedAttribute) throws NamingException {

		System.out.println("=====> Adding Attribute from given DNs ========> ");

		DirContext context = null;

		try {
			context = new InitialDirContext(env);

			for (String userDN : userDNList) {
				try {
					Attribute attribute = new BasicAttribute(modifiedAttribute, MODIFIED_ATTRIBUTE_VALUE);
					ModificationItem[] item = new ModificationItem[1];
					item[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, attribute);
					context.modifyAttributes(userDN, item);

					System.out.println("=====> Attribute is added for User DN : " + userDN);
				} catch (SchemaViolationException | AttributeInUseException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (context != null) {
				try {
					context.close();
				} catch (NamingException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("=====> Attribute is added from given DNs successfully ========> ");

	}

}