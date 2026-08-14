package online.omniroute.mobile.runtime

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import java.io.File

class RuntimePolicyTest {
    @Test
    fun `accepts child path inside managed root`() {
        val root = File("/tmp/omniroute-runtime")
        val policy = RuntimePolicy(homeDirectory = File("/tmp/omniroute-home"), workingDirectory = root)
        val child = policy.validateChildPath(File(root, "logs/runtime.log"))
        assertSame(child, child)
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
