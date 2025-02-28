package com.logicaldoc.core.security.dao;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.logicaldoc.core.AbstractCoreTCase;
import com.logicaldoc.core.PersistenceException;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.security.Menu;
import com.logicaldoc.core.security.Permission;
import com.logicaldoc.core.security.User;

import junit.framework.Assert;

/**
 * Test case for <code>HibernateMenuDAOTest</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 3.0
 */
public class HibernateMenuDAOTest extends AbstractCoreTCase {

	// Instance under test
	private MenuDAO dao;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		// Retrieve the instance under test from spring context. Make sure that
		// it is an HibernateMenuDAO
		dao = (MenuDAO) context.getBean("MenuDAO");
	}

	@Test
	public void testStore() throws PersistenceException {
		Menu menu = new Menu();
		menu.setName("text");
		menu.setParentId(2);
		menu.setMenuGroup(new long[] { 1, 2 });
		Assert.assertTrue(dao.store(menu));

		menu = dao.findById(2000);
		Assert.assertEquals("menu.adminxxx", menu.getName());

		Assert.assertEquals(1, menu.getMenuGroups().size());

		// Load an existing menu and modify it
		menu = dao.findById(9);
		Assert.assertEquals("security", menu.getName());

		dao.store(menu);

		menu = dao.findById(101);
		menu.setName("pippo");
		Assert.assertTrue(dao.store(menu));
		menu = dao.findById(102);
		Assert.assertNotNull(menu);

		menu = dao.findById(101);
		menu.setName("pippo2");
		Assert.assertTrue(dao.store(menu));
		menu = dao.findById(102);

		menu = dao.findById(102);
		Assert.assertTrue(dao.store(menu));
	}

	@Test
	public void testDelete() throws PersistenceException {
		Assert.assertTrue(dao.delete(99));
		Menu menu = dao.findById(99);
		Assert.assertNull(menu);

		DocumentDAO docDao = (DocumentDAO) context.getBean("DocumentDAO");
		docDao.delete(1);

		// Delete a folder with documents
		Assert.assertTrue(dao.delete(103));
		menu = dao.findById(103);
		Assert.assertNull(menu);
	}

	@Test
	public void testFindById() {
		// Try with a menu id
		Menu menu = dao.findById(2);
		Assert.assertNotNull(menu);
		Assert.assertEquals(2, menu.getId());
		Assert.assertEquals("administration", menu.getName());
		Assert.assertEquals("menu.png", menu.getIcon());
		Assert.assertEquals(1, menu.getMenuGroups().size());

		// Try with unexisting id
		menu = dao.findById(99999);
		Assert.assertNull(menu);
	}

	@Test
	public void testFindByName() {
		List<Menu> menus = (List<Menu>) dao.findByName(null, "abc", true);
		Assert.assertNotNull(menus);
		Assert.assertEquals(0, menus.size());

		menus = (List<Menu>) dao.findByName(null, "abc", false);
		Assert.assertNotNull(menus);
		Assert.assertEquals(1, menus.size());

		// Try with unexisting text
		menus = dao.findByName("xxxxx");
		Assert.assertNotNull(menus);
		Assert.assertTrue(menus.isEmpty());
	}

	@Test
	public void testFindByUserNameString() {
		List<Menu> menus = dao.findByUserId(1);
		Assert.assertNotNull(menus);
		Assert.assertEquals(dao.findAllIds().size(), menus.size());

		menus = dao.findByUserId(3);
		Assert.assertNotNull(menus);
		Assert.assertEquals(dao.findAllIds().size(), menus.size());

		// Try with unexisting user
		menus = dao.findByUserId(99);
		Assert.assertNotNull(menus);
		Assert.assertEquals(0, menus.size());
	}

	@Test
	public void testFindByUserId() {
		List<Menu> menus = dao.findByUserId(1, 2, false);
		Assert.assertNotNull(menus);
		Assert.assertEquals(9, menus.size());

		// Try with unexisting user and menus
		menus = dao.findByUserId(1, 999, false);
		Assert.assertNotNull(menus);
		Assert.assertEquals(0, menus.size());

		menus = dao.findByUserId(99, 2, false);
		Assert.assertNotNull(menus);
		Assert.assertEquals(0, menus.size());

		menus = dao.findByUserId(4, 2, false);
		Assert.assertNotNull(menus);
		Assert.assertEquals(1, menus.size());
	}

	@Test
	public void testFindByParentId() {
		List<Menu> menus = dao.findByParentId(2, false);
		Assert.assertNotNull(menus);
		Assert.assertEquals(31, menus.size());

		// Try with unexisting parent
		menus = dao.findByParentId(999, false);
		Assert.assertNotNull(menus);
		Assert.assertEquals(0, menus.size());
	}

	@Test
	public void testIsWriteEnable() {
		Assert.assertTrue(dao.isWriteEnable(2, 1));
		Assert.assertTrue(dao.isWriteEnable(26, 1));
		Assert.assertTrue(dao.isWriteEnable(1200, 4));
		Assert.assertTrue(dao.isWriteEnable(2, 3));
		Assert.assertFalse(dao.isWriteEnable(2, 999));
	}

	@Test
	public void testIsReadEnable() {
		Assert.assertTrue(dao.isReadEnable(2, 1));
		Assert.assertTrue(dao.isReadEnable(26, 1));
		Assert.assertFalse(dao.isReadEnable(2, 22));
		Assert.assertFalse(dao.isReadEnable(2, 999));
		Assert.assertTrue(dao.isReadEnable(1200, 4));
	}

	@Test
	public void testFindMenuIdByUserId() {
		Collection<Long> ids = dao.findMenuIdByUserId(4, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(22, ids.size());
		Assert.assertTrue(ids.contains(104L));
		Assert.assertTrue(ids.contains(1200L));

		// Try with unexisting user
		ids = dao.findMenuIdByUserId(99, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(0, ids.size());
	}

	@Test
	public void testFindIdByUserId() {
		Collection<Long> ids = dao.findIdByUserId(1, 101);
		Assert.assertNotNull(ids);
		Assert.assertEquals(3, ids.size());
		Assert.assertTrue(ids.contains(103L));
		Assert.assertTrue(ids.contains(102L));
		Assert.assertTrue(ids.contains(104L));

		ids = dao.findIdByUserId(4, 101);
		Assert.assertNotNull(ids);
		Assert.assertEquals(2, ids.size());
		Assert.assertTrue(ids.contains(103L));
		Assert.assertTrue(ids.contains(104L));

		ids = dao.findIdByUserId(1, 101);
		Assert.assertNotNull(ids);
		Assert.assertEquals(3, ids.size());

		// Try with unexisting user
		ids = dao.findIdByUserId(99, 101);
		Assert.assertNotNull(ids);
		Assert.assertEquals(0, ids.size());
	}

	@Test
	public void testHasWriteAccess() {
		Menu menu = dao.findById(103);
		Assert.assertTrue(dao.hasWriteAccess(menu, 1));
		Assert.assertTrue(dao.hasWriteAccess(menu, 3));
		Assert.assertFalse(dao.hasWriteAccess(menu, 5));
		menu = dao.findById(103);
		Assert.assertTrue(dao.hasWriteAccess(menu, 4));
		menu = dao.findById(104);
		Assert.assertFalse(dao.hasWriteAccess(menu, 4));
	}

	@Test
	public void testFindByGroupId() {
		Collection<Menu> menus = dao.findByGroupId(1);
		Assert.assertEquals(dao.findAllIds().size(), menus.size());
		menus = dao.findByGroupId(10);
		Assert.assertEquals(0, menus.size());
		menus = dao.findByGroupId(2);
		Assert.assertTrue(menus.contains(dao.findById(104)));
		Assert.assertTrue(menus.contains(dao.findById(1200)));
	}

	@Test
	public void testFindParents() {
		List<Menu> menus = dao.findParents(103);
		Assert.assertEquals(4, menus.size());
		Assert.assertEquals(dao.findById(1), menus.get(0));
		Assert.assertEquals(dao.findById(2), menus.get(1));
		Assert.assertEquals(dao.findById(2000), menus.get(2));
	}

	@Test
	public void testRestore() throws PersistenceException {
		Menu menu = dao.findById(1000);
		Assert.assertNull(menu);
		menu = dao.findById(1100);
		Assert.assertNull(menu);

		dao.restore(1100, true);
		menu = dao.findById(1000);
		Assert.assertNotNull(menu);
		menu = dao.findById(1100);
		Assert.assertNotNull(menu);
	}

	@Test
	public void testFindByNameAndParentId() {
		List<Menu> menus = dao.findByNameAndParentId("%admin%", 2);
		Assert.assertEquals(2, menus.size());
		Assert.assertTrue(menus.contains(dao.findById(99)));
		Assert.assertTrue(menus.contains(dao.findById(2000)));
		menus = dao.findByNameAndParentId("text", 2000);
		Assert.assertEquals(dao.findById(101), menus.get(0));
	}

	@Test
	public void testFindMenuIdByUserIdAndPermission() {
		List<Long> ids = dao.findMenuIdByUserIdAndPermission(4, Permission.WRITE, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(3, ids.size());
		Assert.assertTrue(ids.contains(104L));
		Assert.assertTrue(ids.contains(1200L));
		ids = dao.findMenuIdByUserIdAndPermission(1, Permission.WRITE, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(dao.findAllIds().size(), ids.size());
		ids = dao.findMenuIdByUserIdAndPermission(4, Permission.WRITE, true);
		Assert.assertNotNull(ids);
		Assert.assertEquals(3, ids.size());
		Assert.assertTrue(ids.contains(104L));
		Assert.assertTrue(ids.contains(1200L));
	}

	@Test
	public void testComputePathExtended() {
		Assert.assertEquals("/administration/test", dao.computePathExtended(1200));
		Assert.assertEquals("/administration/menu.adminxxx/text/menu.admin", dao.computePathExtended(103));
	}

	@Test
	public void testFindChildren() {
		List<Menu> dirs = dao.findChildren(101L, 1L);
		Assert.assertNotNull(dirs);
		Assert.assertEquals(3, dirs.size());

		dirs = dao.findChildren(101L, 4L);
		Assert.assertNotNull(dirs);
		Assert.assertEquals(2, dirs.size());

		dirs = dao.findChildren(2L, 4L);
		Assert.assertNotNull(dirs);
		Assert.assertEquals(1, dirs.size());
		Assert.assertTrue(dirs.contains(dao.findById(1200L)));
	}

	@Test
	public void testApplyRightsToTree() {
		User user = new User();
		user.setId(4);

		Menu menu = dao.findById(1041);
		Assert.assertTrue(null == menu.getSecurityRef());

		Assert.assertTrue(dao.applyRightToTree(101));
		menu = dao.findById(104);
		Assert.assertTrue(101 == menu.getSecurityRef());
		menu = dao.findById(1041);
		Assert.assertTrue(101 == menu.getSecurityRef());
	}

	@Test
	public void testCreatePath() throws PersistenceException {
		Menu newMenu = dao.createPath(72L, 1L, Menu.TYPE_CUSTOM_ACTION, "/pippo/pluto/paperino", true);
        Assert.assertNotNull(newMenu);
        Assert.assertEquals("/administration/system/general/logs/pippo/pluto/paperino", dao.computePathExtended(newMenu.getId()));
	}
}