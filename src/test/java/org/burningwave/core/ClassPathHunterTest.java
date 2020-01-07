package org.burningwave.core;

import java.io.Closeable;

import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.bean.Complex;
import org.burningwave.core.classes.ClassCriteria;
import org.burningwave.core.classes.hunter.SearchConfig;
import org.burningwave.core.classes.hunter.SearchConfigForPath;
import org.junit.jupiter.api.Test;

public class ClassPathHunterTest extends BaseTest {
	
	@Test
	public void findAllSubtypeOfTestOne() {
		ComponentSupplier componentSupplier = getComponentSupplier();
		testNotEmpty(
			() -> componentSupplier.getClassPathHunter().findBy(
				SearchConfig.forPaths(
					componentSupplier.getPathHelper().getMainClassPaths()
				).by(
					ClassCriteria.create().byClasses((uploadedClasses, targetClass) ->
						uploadedClasses.get(Closeable.class).isAssignableFrom(targetClass)
					).useClasses(
						Closeable.class
					)
				)
			),
			(result) -> result.getItemsFound()
		);
	}

	@Test
	public void findAllSubtypeOfTestTwo() {
		ComponentSupplier componentSupplier = getComponentSupplier();
		testNotEmpty(
			() -> componentSupplier.getClassPathHunter().findBy(
				SearchConfig.forPaths(
					componentSupplier.getPathHelper().getMainClassPaths()
				).by(
					ClassCriteria.create().byClasses((uploadedClasses, targetClass) ->
						uploadedClasses.get(Complex.Data.Item.class).isAssignableFrom(targetClass)
					).useClasses(
						Complex.Data.Item.class
					)
				)
			),
			(result) -> result.getItemsFound()
		);
	}
	
	@Test
	public void cacheTestOne() {
		ComponentSupplier componentSupplier = getComponentSupplier();
		SearchConfigForPath searchConfig = SearchConfig.forPaths(
			componentSupplier.getPathHelper().getMainClassPaths()
		);
		testNotEmpty(
			() -> componentSupplier.getClassPathHunter().findBy(searchConfig),
			(result) -> result.getItemsFound()
		);
		testNotEmpty(
			() -> componentSupplier.getClassPathHunter().findBy(
				searchConfig.by(
					ClassCriteria.create().byClasses((uploadedClasses, targetClass) ->
						uploadedClasses.get(Closeable.class).isAssignableFrom(targetClass)
					).useClasses(
						Closeable.class
					)
				)
			),
			(result) -> result.getItemsFound()
		);
	}

}
