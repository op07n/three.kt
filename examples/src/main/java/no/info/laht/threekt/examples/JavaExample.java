package no.info.laht.threekt.examples;

import info.laht.threekt.Canvas;
import info.laht.threekt.cameras.PerspectiveCamera;
import info.laht.threekt.controls.OrbitControls;
import info.laht.threekt.geometries.BoxBufferGeometry;
import info.laht.threekt.lights.AmbientLight;
import info.laht.threekt.materials.MeshBasicMaterial;
import info.laht.threekt.materials.MeshPhongMaterial;
import info.laht.threekt.math.Color;
import info.laht.threekt.objects.Mesh;
import info.laht.threekt.renderers.GLRenderer;
import info.laht.threekt.scenes.Scene;

public class JavaExample {

    public static void main(String[] args) {

        try (Canvas canvas = new Canvas()) {

            Scene scene = new Scene();
            PerspectiveCamera camera = new PerspectiveCamera();
            camera.getPosition().z = 5;
            GLRenderer renderer = new GLRenderer(canvas.getWidth(), canvas.getHeight());

            BoxBufferGeometry boxBufferGeometry = new BoxBufferGeometry();
            MeshPhongMaterial boxMaterial = new MeshPhongMaterial();
            boxMaterial.getColor().set(Color.getRoyalblue());

            Mesh box = new Mesh(boxBufferGeometry, boxMaterial);
            scene.add(box);


            MeshBasicMaterial wireframeMaterial = new MeshBasicMaterial();
            wireframeMaterial.getColor().set(0x000000);
            wireframeMaterial.setWireframe(true);
            Mesh wireframe = new Mesh(box.getGeometry().clone(), wireframeMaterial);
            scene.add(wireframe);

            AmbientLight light = new AmbientLight();
            scene.add(light);

            OrbitControls orbitControls = new OrbitControls(camera, canvas);

            while (!canvas.shouldClose()) {

                renderer.render(scene, camera);

                canvas.pollEvents();
                canvas.swapBuffers();

            }

        }

    }

}
