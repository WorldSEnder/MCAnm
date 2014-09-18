#version 410 compatibility

layout (location = 0) in vec4 pPosition; // location 0, buffered as vec3
layout (location = 1) in vec3 pNormal; // location 1, buffered as vec3
layout (location = 2) in vec4 pTexcoords; // location 2, buffered as vec2
layout (location = 3) in uvec4 pBindingIndices; // location 3, buffered as vec4
layout (location = 4) in vec4 pBindingValues; // location 4, buffered as vec4

out gl_PerVertex {
	vec4 gl_Position;
	vec4 gl_FrontColor;
	vec4 gl_TexCoord[1];
};

// Layout std140 guarantees sizeof(mat4)==16
layout (std140, binding = 0) uniform BonesStatic {
	mat4 localsMatrices[255];
};

// Layout std140 guarantees sizeof(mat4)==16
layout (std140, binding = 1) uniform BonesTransform {
	mat4 transformMatrix[255];
};

void DirectionalLight(in int i, in vec3 normal, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular);
void PointLight(in int i, in vec3 eye, in vec3 ecPosition3, in vec3 normal, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular);
void SpotLight(in int i, in vec3 eye, in vec3 ecPosition3, in vec3 normal, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular);

void main(){
	// Transform normal and vertPos with bones
	vec4 transformedPos = vec4(0.0, 0.0, 0.0, 1.0);
	vec3 transformedNormal = vec3(0.0);
	if( pBindingIndices.x == 0xFF &&
		pBindingIndices.y == 0xFF &&
		pBindingIndices.z == 0xFF &&
		pBindingIndices.w == 0xFF) {
		transformedPos = pPosition;
		transformedNormal = pNormal;
	} else {
		for(int i = 0; i < 4; ++i) {
			float value = pBindingValues[i];
			uint binding = pBindingIndices[i];
			if(value <= 0.0 || binding == 0xFF) {
				// If either the binding is '-1' or the strength is zero
				continue;
			}
			mat4 localToWorld = localsMatrices[binding];
			mat4 transformMat = transformMatrix[binding];
			transformedPos +=  value * ((localToWorld * transformMat) * pPosition);
			// The next line contains a right-hand matrix multiplication, we save one transpose (we normally need transposed inverse)
			transformedNormal +=  value * vec3(vec4(pNormal, 0.0) * inverse(localToWorld * transformMat));
		}
	}
	vec4 vertPos = transformedPos;
	vec3 normal = transformedNormal;
	// Apply lightning, no fragment shader.... MINECRAFT AHHHHH
	vertPos = gl_ModelViewMatrix * vertPos;
	normal = gl_NormalMatrix * normal;
	// Let the copy pasta begin! http://www.yaldex.com/open-gl/ch09lev1sec3.html
	vec3 ecPosition3 = vec3(vertPos) / vertPos.w;
	vec3 eye = -normalize(ecPosition3);
	vec4 amb  = vec4(0.0, 0.0, 0.0, 1.0);
	vec4 diff = vec4(0.0, 0.0, 0.0, 1.0);
	vec4 spec = vec4(0.0, 0.0, 0.0, 1.0);
	for(int i = 0; i < gl_MaxLights; ++i) {
		if (gl_LightSource[i].position.w == 0.0)
			DirectionalLight(i, normal, amb, diff, spec);
		else if (gl_LightSource[i].spotCutoff == 180.0)
			PointLight(i, eye, ecPosition3, normal, amb, diff, spec);
		else
			SpotLight(i, eye, ecPosition3, normal, amb, diff, spec);
	}
	vec4 color = gl_FrontLightModelProduct.sceneColor +
			amb * gl_FrontMaterial.ambient +
			diff * gl_FrontMaterial.diffuse +
			spec * gl_FrontMaterial.specular;
	gl_Position = gl_ProjectionMatrix * vertPos;
	gl_FrontColor = color;
	gl_TexCoord[0] = gl_TextureMatrix[0] * pTexcoords;
}

void DirectionalLight(in int i, in vec3 normal, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular)
{
     float nDotVP;         // normal . light direction
     float nDotHV;         // normal . light half vector
     float pf;             // power factor

     nDotVP = max(0.0, dot(normal,
                   normalize(vec3(gl_LightSource[i].position))));
     nDotHV = max(0.0, dot(normal, vec3(gl_LightSource[i].halfVector)));

     if (nDotVP == 0.0)
         pf = 0.0;
     else
         pf = pow(nDotHV, gl_FrontMaterial.shininess);

     ambient  += gl_LightSource[i].ambient;
     diffuse  += gl_LightSource[i].diffuse * nDotVP;
     specular += gl_LightSource[i].specular * pf;
}

void PointLight(in int i, in vec3 eye, in vec3 ecPosition3, in vec3 normal, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular)
{
    float nDotVP;         // normal . light direction
    float nDotHV;         // normal . light half vector
    float pf;             // power factor
    float attenuation;    // computed attenuation factor
    float d;              // distance from surface to light source
    vec3  VP;             // direction from surface to light position
    vec3  halfVector;     // direction of maximum highlights

    // Compute vector from surface to light position
    VP = vec3(gl_LightSource[i].position) - ecPosition3;

    // Compute distance between surface and light position
    d = length(VP);

    // Normalize the vector from surface to light position
    VP = normalize(VP);

    // Compute attenuation
    attenuation = 1.0 / (gl_LightSource[i].constantAttenuation +
                         gl_LightSource[i].linearAttenuation * d +
                         gl_LightSource[i].quadraticAttenuation * d * d);

    halfVector = normalize(VP + eye);

    nDotVP = max(0.0, dot(normal, VP));
    nDotHV = max(0.0, dot(normal, halfVector));

    if (nDotVP == 0.0)
        pf = 0.0;
    else
        pf = pow(nDotHV, gl_FrontMaterial.shininess);

    ambient += gl_LightSource[i].ambient * attenuation;
    diffuse += gl_LightSource[i].diffuse * nDotVP * attenuation;
    specular += gl_LightSource[i].specular * pf * attenuation;
}

void SpotLight(in int i, in vec3 eye, in vec3 ecPosition3, in vec3 normal, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular)
{
    float nDotVP;           // normal . light direction
    float nDotHV;           // normal . light half vector
    float pf;               // power factor
    float spotDot;          // cosine of angle between spotlight
    float spotAttenuation;  // spotlight attenuation factor
    float attenuation;      // computed attenuation factor
    float d;                // distance from surface to light source
    vec3 VP;                // direction from surface to light position
    vec3 halfVector;        // direction of maximum highlights

    // Compute vector from surface to light position
    VP = vec3(gl_LightSource[i].position) - ecPosition3;

    // Compute distance between surface and light position
    d = length(VP);

    // Normalize the vector from surface to light position
    VP = normalize(VP);

    // Compute attenuation
    attenuation = 1.0 / (gl_LightSource[i].constantAttenuation +
                         gl_LightSource[i].linearAttenuation * d +
                         gl_LightSource[i].quadraticAttenuation * d * d);

    // See if point on surface is inside cone of illumination
    spotDot = dot(-VP, normalize(gl_LightSource[i].spotDirection));

    if (spotDot < gl_LightSource[i].spotCosCutoff)
        spotAttenuation = 0.0; // light adds no contribution
    else
        spotAttenuation = pow(spotDot, gl_LightSource[i].spotExponent);

    // Combine the spotlight and distance attenuation.
    attenuation *= spotAttenuation;

    halfVector = normalize(VP + eye);

    nDotVP = max(0.0, dot(normal, VP));
    nDotHV = max(0.0, dot(normal, halfVector));

    if (nDotVP == 0.0)
        pf = 0.0;
    else
        pf = pow(nDotHV, gl_FrontMaterial.shininess);

    ambient  += gl_LightSource[i].ambient * attenuation;
    diffuse  += gl_LightSource[i].diffuse * nDotVP * attenuation;
    specular += gl_LightSource[i].specular * pf * attenuation;
}

