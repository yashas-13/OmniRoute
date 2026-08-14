package online.omniroute.mobile.runtime

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimePolicyTest {
    @Test
    fun `accepts child path inside managed root`() {
        val root = File("/tmp/omniroute-runtime")
        val policy = RuntimePolicy(homeDirectory = File("/tmp/omniroute-home"), workingDirectory = root)
        val child = policy.validateChildPath(File(root, "logs/runtime.log"))
        assertEquals(File(root, "logs/runtime.log").canonicalPath, child.path)
    }

    @Test
    fun `rejects path traversal outside managed root`() {
        val root = File("/tmp/omniroute-runtime")
        val policy = RuntimePolicy(homeDirectory = File("/tmp/omniroute-home"), workingDirectory = root)
        assertFailsWith<IllegalArgumentException> {
            policy.validateChildPath(File(root, "../secrets"))
        }
    }
}
