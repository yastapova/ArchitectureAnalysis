# ArchitectureAnalysis

<p>Originally, I wrote my code to calculate the Core size of a system, since no other libraries or software I found did that yet. I also re-implemented the jDSM method of calculating the propagation cost, as well as added the option to calculate it without self-dependencies, because the jDSM code for this was excruciatingly slow, especially on large systems. I swapped in the jBLAS library for the jScience library to do matrix operations faster. Also, my code includes a simple runner for the JDepend library so that I can obtain presentable data.</p>

<h4>Size</h4>
<ul>
<li>Uses jDSM’s method for DSM creation, so this always has the same number of classes as jDSM.</li>
</ul>
<h4>VFI/VFO</h4>
<ul>
<li>VFI refers to Fan-In Visibility (or number of elements that depend on this one) and VFO refers to Fan-Out Visibility (or number of elements that this one depends on).</li>
<li>The VFI for a particular element is the sum of its column in the visibility matrix, while the VFO is the sum of its row. (The visibility matrix is binary.)</li>
<li>Here, the sum of all VFI and the sum of all VFO are given because reporting the list of individual VFIs and VFOs would be unwieldy and meaningless, especially for large systems.</li>
<li>The sum VFI and sum VFO should be equal in all cases.</li>
</ul>
<h4>Propagation Cost</h4>
<ul>
<li>Propagation cost here has the same meaning as in jDSM.</li>
<li>However, this propagation cost does not include self-dependencies.</li>
<li>This propagation cost is calculated differently in the interest of speed, by summing all VFIs (or all VFOs) and dividing the sum by N^2 (where N is the dimension of the square visibility matrix).</li>
</ul>
<h4>Core Size</h4>
<ul>
<li>The Core (with capital “C”) refers to the dominant cyclic group in a system.</li>
<li>Cyclic groups are groups of components in which each member depends, directly or indirectly, on all other members of the group.</li>
<li>The larger the Core (relative to the system), the worse, because:
<ul>
<li>A large Core creates challenges in architecture and development</li>
<li>It necessitates knowledge and understanding of a large number of interdependencies all at once</li>
</ul></li>
</ul>
