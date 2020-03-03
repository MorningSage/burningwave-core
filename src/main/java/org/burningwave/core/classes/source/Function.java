package org.burningwave.core.classes.source;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

public class Function extends Generator.Abst {
	private Collection<String> outerCode;
	private Integer modifier;
	private TypeDeclaration typesDeclaration;
	private TypeDeclaration returnType;
	private String name;
	private Collection<Variable> parameters;
	private Collection<String> innerCode;
	
	private Function(String name) {
		this.name = name;
	}
	
	public static Function create(String name) {
		return new Function(name);
	}
	
	public static Function create() {
		return new Function(null);
	}
	
	Function setName(String name) {
		this.name = name;
		return this;
	}
	
	public Function addModifier(Integer modifier) {
		if (this.modifier == null) {
			this.modifier = modifier;
		} else {
			this.modifier |= modifier; 
		}
		return this;
	}
	
	public Function setTypeDeclaration(TypeDeclaration typesDeclaration) {
		this.typesDeclaration = typesDeclaration;
		return this;
	}
	
	public Function setReturnType(TypeDeclaration returnType) {
		this.returnType = returnType;
		return this;
	}
	
	public Function addParameter(Variable parameter) {
		this.parameters = Optional.ofNullable(this.parameters).orElseGet(ArrayList::new);
		this.parameters.add(parameter);
		return this;
	}
	
	public Function addOuterCodeRow(String code) {
		this.outerCode = Optional.ofNullable(this.outerCode).orElseGet(ArrayList::new);
		if (!this.outerCode.isEmpty()) {
			this.outerCode.add("\n" + code);
		} else {
			this.outerCode.add(code);
		}
		return this;
	}
	
	public Function addInnerCodeRow(String code) {
		this.innerCode = Optional.ofNullable(this.innerCode).orElseGet(ArrayList::new);
		this.innerCode.add("\n\t" + code);
		return this;
	}
	
	private String getInnerCode() {
		if (innerCode != null) {
			return "{" + getOrEmpty(innerCode) + "\n}";
		}
		return "";
	}

	private String getParametersCode() {
		String paramsCode = "(";
		if (parameters != null) {
			paramsCode += "\n";
			Iterator<Variable> paramsIterator =  parameters.iterator();
			while (paramsIterator.hasNext()) {
				Variable param = paramsIterator.next();
				paramsCode += "\t" + param.make().replace("\n", "\n\t");
				if (paramsIterator.hasNext()) {
					paramsCode += ",\n";
				} else {
					paramsCode += "\n";
				}
			}
		}
		return paramsCode + ")";
	}
	
	Collection<TypeDeclaration> getTypeDeclarations() {
		Collection<TypeDeclaration> types = new ArrayList<>();
		Optional.ofNullable(typesDeclaration).ifPresent(typesDeclaration -> {
			types.addAll(typesDeclaration.getTypeDeclarations());
		});
		Optional.ofNullable(returnType).ifPresent(returnType -> {
			types.addAll(returnType.getTypeDeclarations());
		});
		Optional.ofNullable(parameters).ifPresent(parameters -> {
			parameters.forEach(parameter -> {
				types.addAll(parameter.getTypeDeclarations());
			});
		});;
		return types;
	}
	
	@Override
	public String make() {
		return getOrEmpty(
			Optional.ofNullable(outerCode).map(outerCode ->
				getOrEmpty(outerCode) + "\n"
			).orElseGet(() -> null),
			Optional.ofNullable(modifier).map(mod -> Modifier.toString(this.modifier)).orElseGet(() -> null),
			typesDeclaration,
			returnType,
			name + getParametersCode(),
			getInnerCode()
		);
	}
		
}
