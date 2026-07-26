/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.z3;

import com.microsoft.z3.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Z3SolverLoaderTest {
	// The array creation is not redundant, because the use a more specific type without any generic arguments.
	@SuppressWarnings("RedundantArrayCreation")
	@Test
	void testLoad() {
		assertDoesNotThrow(Z3SolverLoader::loadNativeLibraries);
		try (var context = new Context()) {
			var solver = context.mkSolver();
			var a = context.mkConst("a", context.getIntSort());
			var b = context.mkConst("b", context.getIntSort());
			// Explicit array creation, since {@code Solver#add} is not {@code @SafeVarargs}.
			solver.add(new BoolExpr[]{context.mkEq(a, context.mkInt(3))});
			solver.add(new BoolExpr[]{context.mkEq(b, context.mkMul(context.mkInt(2), a))});
			assertEquals(Status.SATISFIABLE, solver.check());
			var model = solver.getModel();
			var bValue = (IntNum) model.getConstInterp(b);
			assertEquals(6, bValue.getInt());
		}
	}
}
